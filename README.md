# EN: Actionbar Maker — Making custom actionbars
## commands

- `/abm` — open actionbar manager.
- `/abm create "name"` — open actionbar editor
- `/abm delete "name"` — delete actionbar from world folder
- `/abm play "name" @a` — play actionbar with editor settings
- `/abm play "name" @a <fadeIn> <stay> <fadeOut>` — play actionbar with custom settings

Example:

```mcfunction
/abm play hello @a 5 40 10
```

## Where files are stored

- Actionbars in world: `<your world folder>/abm/actionbars/*.json`
- Templates: `config/abm/templates/*.json`
- PNG images (heads): `config/abm/heads/*.png`

## What's done

- GUI-manager `/abm`.
- GUI-editor `/abm create "name"`.
- Saving actionbars in worlds folder
- Delete with GUI and `/abm delete`.
- Play with `/abm play`.
- Settings fadeIn/stay/fadeOut.
- Background.
- Colors for fragments.
- Styles by fragments: bold, italic, подчёркивание, зачёркивание, obfuscated.
- Import and save templates.
- PNG images (heads) from `config/abm/heads`.
- Client overlay draws on higher GUI (chat can't block it), and clear vanilla `/title ... actionbar` to escape conflicts.

# RU: Actionbar Maker — Создание кастомных экшнбаров

## Команды

- `/abm` — открыть менеджер созданных actionbar'ов.
- `/abm create "название"` — открыть редактор нового или уже существующего actionbar'а.
- `/abm delete "название"` — удалить actionbar из папки мира.
- `/abm play "название" @a` — проиграть actionbar с настройками длительности по умолчанию.
- `/abm play "название" @a <fadeIn> <stay> <fadeOut>` — проиграть actionbar с длительностями в тиках.

Пример:

```mcfunction
/abm play "hello" @a 5 40 10
```

## Где хранятся файлы

- Actionbar'ы мира: `<папка мира>/abm/actionbars/*.json`
- Шаблоны: `config/abm/templates/*.json`
- PNG картинки (головы): `config/abm/heads/*.png`

## Что уже реализовано

- GUI-менеджер `/abm`.
- GUI-редактор `/abm create "name"`.
- Сохранение actionbar'ов в папку текущего мира.
- Удаление через GUI и `/abm delete`.
- Проигрывание через `/abm play`.
- Настройки fadeIn/stay/fadeOut.
- Полупрозрачный фон.
- Цвета по фрагментам.
- Стили по фрагментам: жирный, курсив, подчёркивание, зачёркивание, obfuscated.
- Импорт шаблона и сохранение шаблона.
- PNG-головы из `config/abm/heads`.
- Клиентский overlay рисуется выше обычного GUI, а во время показа ABM очищает vanilla `/title ... actionbar`, чтобы он не перебивал кастомный actionbar.