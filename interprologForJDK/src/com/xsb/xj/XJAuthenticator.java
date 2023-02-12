/*
 * XJAuthenticator.java
 *
 * Created on June 10, 2005, 5:08 PM
 *
 */

package com.xsb.xj;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
/**
 *
 * @author crojo
 */
public class XJAuthenticator extends Authenticator {
    XJPasswordDialog dialog;
    
    public PasswordAuthentication getPasswordAuthentication() {
        if (dialog == null) {
            dialog = new XJPasswordDialog(null, true);
        }
        dialog.show(true);
        if (dialog.isOkClicked()) {
            return new PasswordAuthentication(dialog.getUserName(), dialog.getPasswordAsCharArray());
        } else {
            return null;
        }
        
    }
    
}
