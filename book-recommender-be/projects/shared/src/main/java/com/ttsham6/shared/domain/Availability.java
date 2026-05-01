package com.ttsham6.shared.domain;

public enum Availability {
  IN_STOCK(1),
  SHIPPING_TAKES_3_7_DAYS(2),
  SHIPPING_TAKES_3_9_DAYS(3),
  ORDER_FROM_MAKER(4),
  RESERVATION(5),
  CHECKING_STOCK(6);

  int number;

  Availability(int number) {
    this.number = number;
  }
}
