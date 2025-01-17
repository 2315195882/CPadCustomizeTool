package com.saradabar.cpadcustomizetool.data.service;

interface IDeviceOwnerService {
    boolean isDeviceOwnerApp();
    void setUninstallBlocked(String packageName, boolean uninstallBlocked);
    boolean isUninstallBlocked(String packageName);
    boolean tryInstallPackages(String packageName, in List<Uri> uriList);
}