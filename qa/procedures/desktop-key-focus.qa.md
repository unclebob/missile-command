# QA: Desktop key focus

**Task:** `desktop-key-focus`  
**Suite:** desktop-key-focus  
**Gherkin:** `features/desktop-key-focus.feature`

Verify the JVM Quil desktop host can be launched for QA without taking keyboard
focus from the app the user is working in, and without accepting real desktop
keyboard or mouse input while `--no-keyfocus` is active.

## Launch

```sh
bb play 800 600 --qa --no-keyfocus --qa-events tmp/focus.txt
```

`--no-keyfocus` is a documented QA affordance. It may be combined with normal
QA flags. The game window must still be visible; it must not need keyboard or
mouse focus because scripted QA events are consumed inside the sketch loop. Real
desktop keyboard and mouse callbacks are ignored in this mode.

## UI Event Boundary

- Use the documented `bb play` command and the desktop window manager.
- Verify focus from observable OS/application state.
- Do not call project namespaces, core APIs, step handlers, or host internals.

## Procedure

### A. Automated

1. Confirm README documents `--no-keyfocus`.
2. Create a QA event file that waits briefly and quits.
3. Put keyboard focus in a normal desktop app, preferably Terminal.
4. Launch `bb play 800 600 --qa --no-keyfocus --qa-events tmp/focus.txt`.
5. While the game window is visible, assert the previous app still has keyboard focus.
6. Assert real desktop key/mouse activity does not start the game or emit fire telemetry.
7. Assert the process exits successfully after the QA `quit` event.

### B. Manual

8. Put focus in a text editor or terminal and begin typing.
9. Launch `bb play 800 600 --qa --no-keyfocus --qa-events tmp/focus.txt` from the project root.
10. Confirm the game window appears without interrupting typing in the previous app.
11. Confirm typing/clicking outside the game does not start or fire in the game.
12. Confirm scripted QA events still advance and quit the game.

## Pass criteria

- `--no-keyfocus` is documented.
- The visible desktop host does not become the focused app at launch.
- Real desktop keyboard and mouse callbacks do not drive the game.
- Scripted QA events still drive the game.
