对于Android 6.0以下的系统，App在安装时会生成一个权限列表，用户只有在同意之后才能完成App的安装，使得我们想要使用某个App，就要忍受其一些不必要的权限，当然我们也可以对单个权限进行授权或者解除。

在Android 6.0开始，App可以直接安装，然后在运行时请求用户授予权限，系统会弹出一个对话框（这个Dialog不能由开发者定制）让用户决定是否授予某个权限给App。

Google将系统权限分为两类
一类是`Normal Permissions`，这类权限一般不涉及用户隐私，安装即授权，不需要每次使用时都检查权限，比如访问网络、蓝牙、震动等。
另一类是`Dangerous Permission`，一般是涉及到用户隐私的，需要用户进行授权，比如读写SDCard、发送短信、访问通讯录等。

### 一、Normal Permissions

```java
ACCESS_LOCATION_EXTRA_COMMANDS
ACCESS_NETWORK_STATE
ACCESS_NOTIFICATION_POLICY
ACCESS_WIFI_STATE
BLUETOOTH
BLUETOOTH_ADMIN
BROADCAST_STICKY
CHANGE_NETWORK_STATE
CHANGE_WIFI_MULTICAST_STATE
CHANGE_WIFI_STATE
DISABLE_KEYGUARD
EXPAND_STATUS_BAR
GET_PACKAGE_SIZE
INSTALL_SHORTCUT
INTERNET
KILL_BACKGROUND_PROCESSES
MODIFY_AUDIO_SETTINGS
NFC
READ_SYNC_SETTINGS
READ_SYNC_STATS
RECEIVE_BOOT_COMPLETED
REORDER_TASKS
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
REQUEST_INSTALL_PACKAGES
SET_ALARM
SET_TIME_ZONE
SET_WALLPAPER
SET_WALLPAPER_HINTS
TRANSMIT_IR
UNINSTALL_SHORTCUT
USE_FINGERPRINT
VIBRATE
WAKE_LOCK
WRITE_SYNC_SETTINGS
```

### 二、Dangerous Permissions

```java
group:com.google.android.gms.permission.CAR_INFORMATION
 permission:com.google.android.gms.permission.CAR_VENDOR_EXTENSION
  permission:com.google.android.gms.permission.CAR_MILEAGE
  permission:com.google.android.gms.permission.CAR_FUEL

group:android.permission-group.CONTACTS
  permission:android.permission.WRITE_CONTACTS
  permission:android.permission.GET_ACCOUNTS
  permission:android.permission.READ_CONTACTS

group:android.permission-group.PHONE
  permission:android.permission.READ_CALL_LOG
  permission:android.permission.READ_PHONE_STATE
  permission:android.permission.CALL_PHONE
  permission:android.permission.WRITE_CALL_LOG
  permission:android.permission.USE_SIP
  permission:android.permission.PROCESS_OUTGOING_CALLS
  permission:com.android.voicemail.permission.ADD_VOICEMAIL

group:android.permission-group.CALENDAR
  permission:android.permission.READ_CALENDAR
  permission:android.permission.WRITE_CALENDAR

group:android.permission-group.CAMERA
  permission:android.permission.CAMERA

group:android.permission-group.SENSORS
  permission:android.permission.BODY_SENSORS

group:android.permission-group.LOCATION
  permission:android.permission.ACCESS_FINE_LOCATION
  permission:com.google.android.gms.permission.CAR_SPEED
  permission:android.permission.ACCESS_COARSE_LOCATION

group:android.permission-group.STORAGE
  permission:android.permission.READ_EXTERNAL_STORAGE
  permission:android.permission.WRITE_EXTERNAL_STORAGE

group:android.permission-group.MICROPHONE
  permission:android.permission.RECORD_AUDIO

group:android.permission-group.SMS
  permission:android.permission.READ_SMS
  permission:android.permission.RECEIVE_WAP_PUSH
  permission:android.permission.RECEIVE_MMS
  permission:android.permission.RECEIVE_SMS
  permission:android.permission.SEND_SMS
  permission:android.permission.READ_CELL_BROADCASTS
```

可通过以下命令行查看

```java
adb shell pm list permissions -d -g
```

![](http://upload-images.jianshu.io/upload_images/2552605-26dbf36c79f22372?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果App运行在Android 6.0以上的系统中，如果申请某个危险的权限，假设App已被用户授予了同一组的某个危险权限，那么系统会立即授权而不需要用户再次确认。
此外，申请时弹出的对话框之上的权限说明也是对整个权限组的说明，而不是单个权限。
需要注意的是，如果App运行在Android 5.1 (API level 22)或更低级别的设备中，或者targetSdkVersion<=22时（此时设备可以是Android 6.0 (API level 23)或者更高），在系统中仍将采用旧的权限管理策略，系统会要求用户在安装的时候授予所有权限。

运行时权限涉及到的常量与方法有以下几个：

### 三、授权状态标记：

已被授权

```java
PackageManager.PERMISSION_GRANTED
```

不被授权

```java
PackageManager.PERMISSION_DENIED
```

检查某权限或权限组是否已被授予

```java
public static int checkSelfPermission(@NonNull Context context, @NonNull String permission)
```

请求权限

```java
public static void requestPermissions(final @NonNull Activity activity,final @NonNull String[] permissions, final int requestCode)
```

当第一次权限申请被拒绝后，判断再次进行权限申请时是否可以对用户进行权限申请说明

```java
public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,@NonNull String permission)
```

权限申请结果回调

```java
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults)
```

新建一个工程，进行拨打电话以及发送短信权限的申请操作

同样要在AndroidManifest中进行权限声明

```java
 <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.SEND_SMS" />
```

确保此时targetSdkVersion要大于等于23

```java
defaultConfig {
        applicationId "com.example.zy.myapplication"
        minSdkVersion 15
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
```

在布局中添加两个按钮

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context="com.example.zy.myapplication.MainActivity">

    <Button
        android:id="@+id/btn_phone"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="拨打电话" />

    <Button
        android:id="@+id/btn_sms"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="发送短信" />

</LinearLayout>

```

全部代码如下

```java
package com.example.zy.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CALL_PHONE = 1;

    private final int REQUEST_SEND_SMS = 2;

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_phone = (Button) findViewById(R.id.btn_phone);
        Button btn_sms = (Button) findViewById(R.id.btn_sms);
        btn_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检查是否已经被授予权限,如果没有则进行申请，有的话则直接打电话
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //判断此时是否可以向用户说明为什么需要申请权限
                    //说明完之后可以再来调用requestPermissions进行权限申请
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.CALL_PHONE)) {
                        Toast.makeText(MainActivity.this, "我就是需要权限", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
                    }
                } else {
                    callPhone();
                }
            }
        });
        btn_sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                } else {
                    sendSms();
                }
            }
        });
    }

    private void callPhone() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + "10086");
        intent.setData(data);
        startActivity(intent);
    }

    private void sendSms() {
        Toast.makeText(this, "被授予了权限，可以进行发送短信的操作", Toast.LENGTH_SHORT).show();
    }

    //permissions是申请时所包含的所有权限
    //grantResults是对应权限的处理结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CALL_PHONE:
                for (String permission : permissions) {
                    Log.e(TAG + "--Phone", permission);
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callPhone();
                } else {
                    Toast.makeText(this, "拒绝授权", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_SEND_SMS:
                for (String permission : permissions) {
                    Log.e(TAG + "--SMS", permission);
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms();
                } else {
                    Toast.makeText(this, "拒绝授权", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}

```
确保此时运行的设备系统版本为6.0或更高版本

![](http://upload-images.jianshu.io/upload_images/2552605-763892fad132c56c?imageMogr2/auto-orient/strip)