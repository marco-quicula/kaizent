package br.com.refatorado.kaizent.run;

/** The reviewer's decision on a proposal. Spec 0002 records {@code APPROVED} only. */
public enum VerdictType {
  APPROVED,
  APPROVED_WITH_EDITS,
  REJECTED
}
