package org.cerion.tasklist.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class AlertDialogFragment extends DialogFragment
{

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    public static AlertDialogFragment newInstance(String sTitle, String sMessage)
    {
        AlertDialogFragment frag = new AlertDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE, sTitle);
        args.putString(MESSAGE, sMessage);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        Bundle bundle = getArguments();
        alert.setTitle(bundle.getString(TITLE));
        alert.setMessage(bundle.getString(MESSAGE));
        alert.setPositiveButton("OK", null);

        return alert.create();
    }
}
