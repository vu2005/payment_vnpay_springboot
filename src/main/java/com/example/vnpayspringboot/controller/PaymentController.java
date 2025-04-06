package com.example.vnpayspringboot.controller;

import com.example.vnpayspringboot.config.VnpayConfig;
import com.example.vnpayspringboot.util.HmacSha512Util;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class PaymentController {

    @GetMapping("/")
    public String showPaymentForm() {
        return "payment"; // Trả về template payment.html
    }

    @PostMapping("/payment/create")
    public String createPayment(@RequestParam("amount") long amount, RedirectAttributes redirectAttributes) {
        try {
            // Các tham số bắt buộc
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_OrderInfo = "Thanh toan don hang test";
            String vnp_TxnRef = String.valueOf(System.currentTimeMillis()); // Mã giao dịch duy nhất
            String vnp_IpAddr = "127.0.0.1"; // IP của client (có thể thay đổi khi deploy)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // Đảm bảo giờ Việt Nam (UTC+7)
            String vnp_CreateDate = sdf.format(new Date());
            String vnp_Amount = String.valueOf(amount * 100L); // Nhân 100 để đổi sang cent
            String vnp_Locale = "vn";
            String vnp_ReturnUrl = VnpayConfig.VNP_RETURN_URL;
            String vnp_BankCode = "NCB"; // Ngân hàng test
            String vnp_OrderType = "billpayment"; // Thêm tham số bắt buộc

            // Sử dụng TreeMap để đảm bảo thứ tự tham số
            Map<String, String> vnp_Params = new TreeMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", VnpayConfig.VNP_TMN_CODE);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType); // Thêm OrderType
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_BankCode", vnp_BankCode);

            // Tạo chuỗi query
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                        .append("&");
            }
            String queryUrl = query.substring(0, query.length() - 1); // Xóa "&" cuối cùng

            // Tạo chữ ký
            String vnp_SecureHash = HmacSha512Util.hmacSHA512(VnpayConfig.VNP_HASH_SECRET, queryUrl);
            String paymentUrl = VnpayConfig.VNP_URL + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

            // Log để debug
            System.out.println("Payment URL: " + paymentUrl);

            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Lỗi khi tạo URL thanh toán: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/payment/return")
    public String paymentReturn(@RequestParam Map<String, String> params, Model model) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        Map<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && !entry.getKey().equals("vnp_SecureHash")) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        String query = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse("");
        String signValue = HmacSha512Util.hmacSHA512(VnpayConfig.VNP_HASH_SECRET, query);

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(params.get("vnp_ResponseCode"))) {
                model.addAttribute("message", "Giao dịch thành công!");
                model.addAttribute("txnRef", params.get("vnp_TxnRef"));
                model.addAttribute("amount", Long.parseLong(params.get("vnp_Amount")) / 100);
            } else {
                model.addAttribute("message", "Giao dịch thất bại! Mã lỗi: " + params.get("vnp_ResponseCode"));
            }
        } else {
            model.addAttribute("message", "Chữ ký không hợp lệ!");
        }
        return "payment";
    }
}