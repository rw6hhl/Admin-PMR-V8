btnChanList.setOnClickListener(v -> {
    try {
        AppLog.add("SettingsActivity: нажата «Список» → ChanListActivity");
        startActivity(new Intent(this, ChanListActivity.class));
        AppLog.add("SettingsActivity: startActivity(ChanListActivity) — OK");
    } catch (Exception e) {
        AppLog.add("SettingsActivity: ОШИБКА startActivity(ChanListActivity) — "
                + e.getClass().getSimpleName() + ": " + e.getMessage());
        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
});