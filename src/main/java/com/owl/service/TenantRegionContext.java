package com.owl.service;

public final class TenantRegionContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();
    private TenantRegionContext() {}
    public static void setOverrideRegion(String r) { if (r == null) TL.remove(); else TL.set(r); }
    public static String getOverrideRegion() { return TL.get(); }
    public static void clear() { TL.remove(); }
}

