package dev.claudevisuals;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Меню функций: категории, поиск, список функций и панель настроек выбранной функции. */
public class MenuScreen extends Screen {
    static final String[] CATS = {"Все", "HUD", "Визуал", "Движение", "Оптимизация", "Таб"};

    private final Screen parent;
    private String cat = "Все", query = "";
    private int page, x0, y0, pw, ph, rows = 1, total;
    private Mods.Mod sel;
    private EditBox search;
    private final List<AbstractWidget> listW = new ArrayList<>(), setW = new ArrayList<>(), catW = new ArrayList<>();

    public MenuScreen(Screen parent) {
        super(Component.literal("ClaudeVisuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        listW.clear(); setW.clear(); catW.clear();
        pw = Math.min(600, width - 12);
        ph = Math.min(340, height - 12);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;

        search = new EditBox(font, x0 + pw - 176, y0 + 9, 166, 18, Component.literal("Поиск"));
        search.setHint(Component.literal("Поиск функций…"));
        search.setValue(query);
        search.setResponder(s -> { query = s; page = 0; refreshList(); });
        addRenderableWidget(search);
        setInitialFocus(search);

        for (int i = 0; i < CATS.length; i++) {
            final String c = CATS[i];
            Button b = Button.builder(Component.literal(c), btn -> { cat = c; page = 0; refreshList(); })
                .bounds(x0 + 6, y0 + 46 + i * 22, 90, 20).build();
            catW.add(b);
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) { page--; refreshList(); } })
            .bounds(x0 + 104, y0 + ph - 30, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { if ((page + 1) * rows < total) { page++; refreshList(); } })
            .bounds(x0 + 130, y0 + ph - 30, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
            .bounds(x0 + pw - 84, y0 + ph - 30, 76, 18).build());

        refreshList();
        refreshSettings();
    }

    private List<Mods.Mod> filtered() {
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Mods.Mod> out = new ArrayList<>();
        for (Mods.Mod m : Mods.ALL) {
            if (!cat.equals("Все") && !m.cat.equals(cat)) continue;
            if (!q.isEmpty()) {
                boolean hit = m.name.toLowerCase(Locale.ROOT).contains(q) || m.desc.toLowerCase(Locale.ROOT).contains(q)
                    || m.cat.toLowerCase(Locale.ROOT).contains(q);
                for (Mods.Setting s : m.s) if (s.name.toLowerCase(Locale.ROOT).contains(q)) hit = true;
                if (!hit) continue;
            }
            out.add(m);
        }
        return out;
    }

    private static Component state(Mods.Mod m) {
        return Component.literal(m.on ? "ВКЛ" : "ВЫКЛ").withStyle(m.on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private void refreshList() {
        for (AbstractWidget w : listW) removeWidget(w);
        listW.clear();
        for (int i = 0; i < catW.size(); i++) {
            boolean on = CATS[i].equals(cat);
            catW.get(i).setMessage(on ? Component.literal(CATS[i]).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD) : Component.literal(CATS[i]));
        }
        List<Mods.Mod> l = filtered();
        total = l.size();
        rows = Math.max(1, (ph - 46 - 38) / 20);
        if (page * rows >= total) page = Math.max(0, (total - 1) / rows);
        for (int i = 0; i < rows; i++) {
            int k = page * rows + i;
            if (k >= total) break;
            final Mods.Mod m = l.get(k);
            Button nb = Button.builder(Component.literal(m.name), b -> { sel = m; refreshSettings(); })
                .bounds(x0 + 104, y0 + 46 + i * 20, 146, 18).build();
            Button tb = Button.builder(state(m), b -> { m.on = !m.on; b.setMessage(state(m)); refreshSettings(); Mods.save(); })
                .bounds(x0 + 252, y0 + 46 + i * 20, 48, 18).build();
            listW.add(nb); listW.add(tb);
            addRenderableWidget(nb); addRenderableWidget(tb);
        }
    }

    private Component togLabel() {
        return Component.literal(sel.on ? "Включено" : "Выключено").withStyle(sel.on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private void refreshSettings() {
        for (AbstractWidget w : setW) removeWidget(w);
        setW.clear();
        if (sel == null) return;
        int rx = x0 + 312, rw = pw - 312 - 8, y = y0 + 70;
        Button tog = Button.builder(togLabel(), b -> { sel.on = !sel.on; b.setMessage(togLabel()); refreshList(); Mods.save(); })
            .bounds(rx, y, rw, 18).build();
        setW.add(tog); addRenderableWidget(tog);
        y += 24;
        for (Mods.Setting s : sel.s) {
            AbstractWidget w;
            if (s.type == 1) {
                w = new Sl(rx, y, rw, s);
            } else {
                w = Button.builder(Component.literal(s.name + ": " + s.fmt()), b -> { s.cycle(); b.setMessage(Component.literal(s.name + ": " + s.fmt())); })
                    .bounds(rx, y, rw, 18).build();
            }
            setW.add(w); addRenderableWidget(w);
            y += 20;
        }
    }

    // ---------- отрисовка ----------
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        drawPanel(g);
    }

    private void drawPanel(GuiGraphics g) {
        long t = System.currentTimeMillis();
        for (int i = 1; i <= 4; i++) g.fill(x0 - i, y0 - i, x0 + pw + i, y0 + ph + i, Hud.a(0xDC7A5A, 14 + (int) (8 * Math.sin(t / 500.0))));
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xF00B0D16);
        g.fillGradient(x0, y0, x0 + pw, y0 + 40, 0xFF1C2036, 0xF00B0D16);
        for (int sx = 0; sx < pw; sx += 4) {
            double k = 0.5 + 0.5 * Math.sin(t / 400.0 + sx * 0.03);
            g.fill(x0 + sx, y0, x0 + Math.min(pw, sx + 4), y0 + 2, Hud.mix(0xFFDC7A5A, 0xFF7C5CFF, k));
        }
        g.fill(x0 + 101, y0 + 44, x0 + 102, y0 + ph - 6, 0x28FFFFFF);
        g.fill(x0 + 306, y0 + 44, x0 + 307, y0 + ph - 6, 0x28FFFFFF);

        Icon.draw(g, x0 + 10, y0 + 8 + (int) Math.round(Math.sin(t / 300.0) * 1.5), 2);
        String title = "ClaudeVisuals";
        int tx = x0 + 40;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int col = Hud.mix(0xFFDC7A5A, 0xFF7C5CFF, 0.5 + 0.5 * Math.sin(t / 350.0 + i * 0.5));
            g.drawString(font, ch, tx, y0 + 9, col, true);
            tx += font.width(ch);
        }
        g.drawString(font, "by ClaudeCoder · ClaudeCode · smerk3r", x0 + 40, y0 + 21, 0xFF8A93AD, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int pages = Math.max(1, (total + rows - 1) / rows);
        g.drawString(font, (page + 1) + "/" + pages + " · " + total, x0 + 160, y0 + ph - 25, 0xFF8A93AD, false);
        int rx = x0 + 312;
        if (sel != null) {
            g.drawString(font, sel.name, rx, y0 + 48, 0xFFFFFFFF, true);
            g.drawString(font, font.plainSubstrByWidth(sel.desc, pw - 312 - 8), rx, y0 + 59, 0xFF8A93AD, false);
        } else {
            g.drawString(font, "Выберите функцию слева", rx, y0 + 60, 0xFF8A93AD, false);
        }
        if (total == 0) g.drawString(font, "Ничего не найдено", x0 + 108, y0 + 60, 0xFF8A93AD, false);
        g.drawString(font, "Right Shift — открыть меню · Esc — закрыть", x0 + 8, y0 + ph - 10, 0xFF5A6280, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Mods.save();
        minecraft.setScreen(parent);
    }

    /** Слайдер для числовых настроек. */
    private static final class Sl extends AbstractSliderButton {
        private final Mods.Setting s;

        Sl(int x, int y, int w, Mods.Setting s) {
            super(x, y, w, 18, Component.empty(), (s.n - s.min) / (s.max - s.min));
            this.s = s;
            updateMessage();
        }

        @Override
        protected void updateMessage() { setMessage(Component.literal(s.name + ": " + s.fmt())); }

        @Override
        protected void applyValue() {
            double v = s.min + value * (s.max - s.min);
            v = Math.round(v / s.step) * s.step;
            s.n = Math.max(s.min, Math.min(s.max, v));
        }
    }
}
