// В методе setupListeners()
btnPTT.setOnTouchListener((v, event) -> {
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            imgRed.setVisibility(View.VISIBLE);
            sendBroadcast(new Intent("PMR_START_RECORDING"));
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            imgRed.setVisibility(View.INVISIBLE);
            sendBroadcast(new Intent("PMR_STOP_RECORDING"));
            return true;
    }
    return false;
});