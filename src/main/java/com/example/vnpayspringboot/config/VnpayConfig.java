package com.example.vnpayspringboot.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class VnpayConfig {
    public static final String VNP_TMN_CODE = "BQMKV9RT";
    public static final String VNP_HASH_SECRET = "318MDPE32TPEE1Z1X2RZGW6K5ADEDFST";
    public static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_RETURN_URL = "http://localhost:8080/payment/return";
}