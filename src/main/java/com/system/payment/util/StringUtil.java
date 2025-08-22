package com.system.payment.util;

public class StringUtil {

    public String safe(String s) { return s == null ? "" : s; }

    public String orDefault(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
}
