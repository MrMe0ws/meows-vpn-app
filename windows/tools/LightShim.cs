// Обёртка для WiX light.exe: добавляет -sval (пропуск ICE-валидации, которая падает без доступа к службе Windows Installer).
// jpackage ищет light.exe в PATH/папке WiX; build.ps1 кладёт эту обёртку вместо него, а оригинал — как light-real.exe.
using System;
using System.Diagnostics;
using System.IO;

static class LightShim
{
    static int Main()
    {
        string dir = Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location);
        string real = Path.Combine(dir, "light-real.exe");
        // исходная командная строка без имени программы
        string cl = Environment.CommandLine;
        int i = cl[0] == '"' ? cl.IndexOf('"', 1) + 1 : cl.IndexOf(' ');
        string rest = i > 0 && i < cl.Length ? cl.Substring(i).TrimStart() : "";
        // -sval только для настоящей линковки (есть -out); проверка версии (`light.exe -?`) jpackage оставляем как есть
        string args = rest.Contains("-out") ? "-sval " + rest : rest;
        var psi = new ProcessStartInfo(real, args) { UseShellExecute = false };
        using (var p = Process.Start(psi)) { p.WaitForExit(); return p.ExitCode; }
    }
}