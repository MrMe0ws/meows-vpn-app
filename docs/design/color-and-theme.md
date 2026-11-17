# Цвет и тема

## Откуда цвета
- `MeowsTheme` (`ui/theme/Theme.kt`) собирает `ColorScheme`:
  1. **Из обоев** (`dynamicLightColorScheme/dynamicDarkColorScheme`), Android 12+, если включено в «Оформление».
  2. **Из палитры** — seed-цвет → схема библиотекой `material-kolor` (`PaletteStyle.Vibrant`). Палитры — `ui/theme/Palettes.kt`.
- Базовая палитра **«Мяу-зелёный»** (`#6FD321`) — глаза кота. Это seed, а не конечный цвет: оттенки строит схема.
- AMOLED: в тёмной теме фон и поверхности → чёрный (`ColorScheme.amoled()`).

## Роли — что для чего
| Роль | Использование у нас |
|---|---|
| `primary` / `onPrimary` | главное действие, кольцо подключения, выбранный элемент (галочка) |
| `primaryContainer` | выбранная строка сервера, быстрый пинг, полоса трафика |
| `secondaryContainer` | иконки-кружки в настройках, фон свайп-действия, «пустое состояние» |
| `tertiaryContainer` | средний пинг, акценты второго порядка |
| `errorContainer` / `error` | нет ответа, срок подписки истекает, опасные действия (удалить) |
| `surface` | фон экранов |
| `surfaceContainer(Low/High/Highest)` | карточки, группы настроек, шапка подписки, строки списков — чем выше «уровень», тем светлее/контрастнее |
| `onSurface` / `onSurfaceVariant` | основной / вторичный текст |
| `outlineVariant` | разделители, трек кольца |

Правило пар: фон `X` → текст/иконки `onX`. Не смешиваем (например, `primaryContainer` + `onSurface`).

## Статусы (смысл → цвет)
- Подключено — `primary` (кольцо + текст + свечение). Подключаемся — `tertiary → primary` вращающаяся дуга. Отключено — нейтральный `outlineVariant` + спящий кот.
- Пинг: `<150 мс` primaryContainer, `<400` tertiaryContainer, иначе или «—» errorContainer, `UDP` — нейтральный.

## Темы
- Режим: Авто / Светлая / Тёмная (хранится в prefs `theme`). Палитра — `palette`, из обоев — `dynamic`, AMOLED — `amoled`.
- Иконки статус-бара переключаются в `MeowsTheme` (SideEffect), цвет системных панелей — прозрачный (edge-to-edge).

## Иллюстрации
Логотип и кот — растровые (`logo_cat.webp`, `sleep_cat.webp`) и не перекрашиваются. Иконка приложения — adaptive + monochrome (themed icons, Android 13+).
