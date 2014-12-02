package edu.umich.eecs.lab11.gateway;

import android.preference.EditTextPreference;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

public class BetterEditText extends EditTextPreference {

    public BetterEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BetterEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BetterEditText(Context context) {
        super(context);
    }

    // According to ListPreference implementation
    @Override
    public CharSequence getSummary() {
        String text = getText();
        if (TextUtils.isEmpty(text)) {
            return getEditText().getHint();
        } else {
            CharSequence summary = super.getSummary();
            if (summary != null) {
                return String.format(summary.toString(), text);
            } else {
                return null;
            }
        }
    }

}
