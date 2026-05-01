package com.ttsham6.shared.domain;

public enum PostageFlag {
  SHIPPING_FEE_NOT_INCLUDED(0),
  FREE_SHIPPING_OM_DELIVERY(1),
  FREE_SHIPPING(2);

  final int number;

  PostageFlag(int number) {
    this.number = number;
  }
}
