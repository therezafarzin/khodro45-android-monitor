# مانیتور خودرو45

پروژهٔ native Kotlin/Compose برای پایش آگهی‌های حساب قانونی خودتان در خودرو45 است. هیچ خرید، پیشنهاد قیمت یا پرداختی انجام نمی‌دهد.

## وضعیت این تحویل

Android Studio لازم نیست. پروژه با ابزار خط فرمان روی macOS ساخته و بررسی شده است. APK امضاشدهٔ release در ریشهٔ پروژه با نام `Khodro45-Monitor.apk` قرار دارد. برنامه با ورود قانونی WebView به خودرو45، endpoint تأییدشدهٔ `/api/v1/auctions/`، Room، پروفایل URL، سرویس foreground، اعلان‌ها و اجرای بعد از boot کار می‌کند.

## ساخت APK

در Terminal و بدون Android Studio، از ریشهٔ پروژه اجرا کنید:

```bash
./gradlew clean test lint assembleDebug assembleRelease
```

APK debug نصب‌شدنی در `app/build/outputs/apk/debug/app-debug.apk` است. نسخهٔ آمادهٔ انتقال نیز `Khodro45-Monitor.apk` است. خروجی release فعلی unsigned است؛ برای update پایدارِ release باید یک signingConfig شخصی خارج از repository اضافه شود. debug APK با همان کلید debug تا زمانی که همان کلید روی این Mac باقی بماند قابل update است.

## نصب و راه‌اندازی Samsung

APK را به گوشی منتقل کنید، در تنظیمات برای برنامهٔ فایل/مرورگر «Install unknown apps» را مجاز کنید، نصب کنید، سپس مجوز اعلان را بدهید. در One UI مسیر Settings → Apps → مانیتور خودرو45 → Battery را روی **Unrestricted** قرار دهید و اعلان‌های برنامه را فعال نگه دارید. یک لینک فیلتر واقعی سایت را در برنامه وارد کنید، سپس دکمهٔ شروع را بزنید.

اگر USB debugging فعال است، نصب با یک فرمان انجام می‌شود:

```bash
adb install -r Khodro45-Monitor.apk
```

برای اجرای خودکار بعد از روشن‌شدن گوشی، گزینهٔ مربوطه را در آیندهٔ صفحهٔ Settings فعال کنید؛ Android ممکن است پس از reboot شروع فوری سرویس را محدود کند.

## امنیت

رمز، cookie، token و Bot Token را در repository یا log قرار ندهید. ورود را فقط در صفحهٔ اصلی خودرو45 انجام دهید. راهنمای خطاهای session/API در Diagnostics ذخیره می‌شود، بدون secret.
