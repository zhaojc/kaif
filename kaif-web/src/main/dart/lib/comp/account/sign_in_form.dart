library sign_in_form;

import 'dart:html';
import 'package:kaif_web/model.dart';
import 'package:kaif_web/util.dart';

class SignInForm {
  final Element elem;
  final AccountService accountService;
  final AccountSession accountSession;
  Alert alert;

  SignInForm(this.elem, this.accountService, this.accountSession) {
    elem.onSubmit.listen(_login);
    alert = new Alert.append(elem);
  }

  void _login(Event e) {
    e
      ..preventDefault()
      ..stopPropagation();

    TextInputElement nameInput = elem.querySelector('#nameInput');
    TextInputElement passwordInput = elem.querySelector('#passwordInput');
    CheckboxInputElement rememberMeInput = elem.querySelector('#rememberMeInput');
    SubmitButtonInputElement submit = elem.querySelector('[type=submit]');
    submit.disabled = true;

    alert.hide();
    accountService.authenticate(nameInput.value, passwordInput.value)//
    .then((AccountAuth accountAuth) {
      accountSession.save(accountAuth, rememberMe:rememberMeInput.checked);
      //TODO handle ?from=
      route.gotoHome();
    }).catchError((e) {
      // server should use `account.AuthenticateFailException` too because domain exception
      // but if server has some internal runtime exception (should be bug). we still
      // need to hide the root cause (because use may find our weak point)
      alert.renderError(i18n('account.AuthenticateFailException'));
    }).whenComplete(() {
      submit.disabled = false;
    });

  }

}