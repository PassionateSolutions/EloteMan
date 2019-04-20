package com.passionatesolutions.app.eloteman.HelpDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import com.passionatesolutions.app.eloteman.R;

public class CustomerHelpDialog extends AppCompatDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        alertBuilder.setTitle(R.string.help_dialog_title)
                .setMessage(R.string.customer_alert_text)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    //Empty to close the dialog
                    }
                });

        return alertBuilder.create();
    }
}