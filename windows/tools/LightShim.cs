// Обёртка для WiX light.exe. Делает две вещи:
//  1. добавляет -sval (пропуск ICE-валидации, которая падает без доступа к службе Windows Installer);
//  2. после обычной линковки (английский MSI) собирает ту же программу ещё раз на русском: MEOWS_RU_MSI — куда положить русский MSI,
//     MEOWS_RU_WXL — русские строки jpackage. Из пары MSI build.ps1 делает языковой переход (ru.mst) для Setup.exe.
// jpackage ищет light.exe в PATH/папке WiX; build.ps1 кладёт эту обёртку вместо него, а оригинал — как light-real.exe.
using System;
using System.Diagnostics;
using System.IO;
using System.Text.RegularExpressions;

static class LightShim
{
    static int Run(string exe, string args)
    {
        var psi = new ProcessStartInfo(exe, args) { UseShellExecute = false };
        using (var p = Process.Start(psi)) { p.WaitForExit(); return p.ExitCode; }
    }

    static int Main()
    {
        string dir = Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location);
        string real = Path.Combine(dir, "light-real.exe");
        // исходная командная строка без имени программы
        string cl = Environment.CommandLine;
        int i = cl[0] == '"' ? cl.IndexOf('"', 1) + 1 : cl.IndexOf(' ');
        string rest = i > 0 && i < cl.Length ? cl.Substring(i).TrimStart() : "";
        // -sval только для настоящей линковки (есть -out); проверка версии (`light.exe -?`) jpackage оставляем как есть
        bool link = rest.Contains("-out");
        // фирменные картинки установщика (баннер и боковая панель с лапкой) — переменные WiX, их задаёт build.ps1
        string banner = Environment.GetEnvironmentVariable("MEOWS_BANNER_BMP");
        string dialogBmp = Environment.GetEnvironmentVariable("MEOWS_DIALOG_BMP");
        string art = "";
        if (link && !string.IsNullOrEmpty(banner) && File.Exists(banner)) art += " -dWixUIBannerBmp=" + banner;
        if (link && !string.IsNullOrEmpty(dialogBmp) && File.Exists(dialogBmp)) art += " -dWixUIDialogBmp=" + dialogBmp;
        int code = Run(real, link ? "-sval" + art + " " + rest : rest);

        string ruWxl = Environment.GetEnvironmentVariable("MEOWS_RU_WXL");
        string ruOut = Environment.GetEnvironmentVariable("MEOWS_RU_MSI");
        if (code == 0 && link && !string.IsNullOrEmpty(ruWxl) && !string.IsNullOrEmpty(ruOut) && File.Exists(ruWxl))
        {
            Directory.CreateDirectory(Path.GetDirectoryName(ruOut));
            string ru = Regex.Replace(rest, @"-out\s+\S+", "-out " + ruOut);
            ru = ru.Replace("-cultures:en-us", "-cultures:ru-ru");
            ru = Regex.Replace(ru, @"-loc\s+\S+MsiInstallerStrings_en\.wxl", "-loc " + ruWxl);
            int c2 = Run(real, "-sval" + art + " " + ru);
            if (c2 != 0) Console.Error.WriteLine("WARN: русская сборка установщика не получилась, код " + c2);
        }
        return code;
    }
}
