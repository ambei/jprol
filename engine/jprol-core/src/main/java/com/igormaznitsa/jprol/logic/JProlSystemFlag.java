package com.igormaznitsa.jprol.logic;

import com.igormaznitsa.jprol.data.Term;
import com.igormaznitsa.jprol.data.TermType;
import com.igormaznitsa.jprol.data.Terms;

import java.util.Locale;
import java.util.Optional;

import static com.igormaznitsa.jprol.data.Terms.*;
import static java.util.Arrays.stream;

public enum JProlSystemFlag {
  VERIFY(false, Terms.newAtom("verify"), TRUE),
  VERSION_DATA(true, Terms.newAtom("version_data"), newStruct(newAtom("jprol"), new Term[] {newLong(2), newLong(0), newLong(0), NULL_LIST}));

  private final Term nameTerm;
  private final Term defaultValue;
  private final boolean readOnly;

  JProlSystemFlag(final boolean readOnly, final Term name, final Term defaultValue) {
    this.nameTerm = name;
    this.readOnly = readOnly;
    this.defaultValue = defaultValue;
  }

  public Term getNameTerm() {
    return this.nameTerm;
  }

  public static Optional<JProlSystemFlag> find(final Term term) {
    final String termText = term.getTermType() != TermType.ATOM ? null : term.getText().toUpperCase(Locale.ENGLISH);

    Optional<JProlSystemFlag> result = Optional.empty();
    if (termText != null) {
      result = stream(JProlSystemFlag.values())
          .filter(x -> x.name().equals(termText))
          .findFirst();
    }
    return result;
  }

  public boolean isReadOnly() {
    return this.readOnly;
  }

  public Term getDefaultValue() {
    return this.defaultValue;
  }
}
