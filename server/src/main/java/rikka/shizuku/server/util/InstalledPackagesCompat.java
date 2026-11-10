package rikka.shizuku.server.util;

import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import rikka.hidden.compat.PackageManagerApis;

public final class InstalledPackagesCompat {

    private static final int ANDROID_17 = 37;

    private InstalledPackagesCompat() {
    }

    public static List<PackageInfo> getInstalledPackages(long flags, int userId)
            throws RemoteException, ReflectiveOperationException {
        return getInstalledPackages(flags, userId, ServiceManager.getService("package"));
    }

    @SuppressWarnings("unchecked")
    public static List<PackageInfo> getInstalledPackages(long flags, int userId, IBinder binder)
            throws RemoteException, ReflectiveOperationException {
        if (Build.VERSION.SDK_INT < ANDROID_17) {
            ParceledListSlice<PackageInfo> slice = PackageManagerApis.getInstalledPackages(flags, userId);
            return slice == null ? Collections.emptyList() : slice.getList();
        }

        // Use the device's framework proxy so Binder unmarshals the actual return type.
        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Object pm = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        if (pm == null) throw new IllegalStateException("Package manager service is unavailable");
        Object result;
        try {
            result = pm.getClass().getMethod("getInstalledPackages", long.class, int.class)
                    .invoke(pm, flags, userId);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RemoteException) throw (RemoteException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
        if (result == null) return Collections.emptyList();

        String type = result.getClass().getName();
        Object list;
        if ("android.content.pm.PackageInfoList".equals(type)) {
            try {
                list = result.getClass().getField("list").get(result);
            } catch (NoSuchFieldException e) {
                list = result.getClass().getMethod("getList").invoke(result);
            }
        } else if ("android.content.pm.ParceledListSlice".equals(type)) {
            list = result.getClass().getMethod("getList").invoke(result);
        } else {
            throw new IllegalStateException("Unsupported getInstalledPackages return type: " + type);
        }
        return list == null ? Collections.emptyList() : (List<PackageInfo>) list;
    }

    public static List<PackageInfo> getInstalledPackagesNoThrow(long flags, int userId) {
        try {
            return getInstalledPackages(flags, userId);
        } catch (Exception e) {
            Log.w("InstalledPackagesCompat", "Cannot enumerate installed packages", e);
            return Collections.emptyList();
        }
    }
}
