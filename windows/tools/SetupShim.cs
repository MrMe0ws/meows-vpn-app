// Meows-Setup.exe: оболочка над MSI. Внутри — английский MSI и русский языковой переход (ru.mst). Язык интерфейса Windows — русский →
// ставим с переходом (установщик на русском), любой другой → английский. Параметры (например /passive, /qn) передаются в msiexec.
using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Reflection;

static class Setup
{
    static void Extract(string resource, string path)
    {
        using (var s = Assembly.GetExecutingAssembly().GetManifestResourceStream(resource))
        using (var f = File.Create(path)) s.CopyTo(f);
    }

    static int Main(string[] argv)
    {
        string tmp = Path.Combine(Path.GetTempPath(), "MeowsSetup-" + Guid.NewGuid().ToString("N").Substring(0, 8));
        Directory.CreateDirectory(tmp);
        try
        {
            string msi = Path.Combine(tmp, "Meows.msi");
            Extract("meows.msi", msi);
            string args = "/i \"" + msi + "\"";
            if (CultureInfo.CurrentUICulture.TwoLetterISOLanguageName == "ru")
            {
                string mst = Path.Combine(tmp, "ru.mst");
                Extract("ru.mst", mst);
                args += " TRANSFORMS=\"" + mst + "\"";
            }
            if (argv.Length > 0) args += " " + string.Join(" ", argv);
            using (var p = Process.Start(new ProcessStartInfo("msiexec.exe", args) { UseShellExecute = false }))
            {
                p.WaitForExit();
                return p.ExitCode;
            }
        }
        finally { try { Directory.Delete(tmp, true); } catch { } }
    }
}
