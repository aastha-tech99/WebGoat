/*
 * SPDX-FileCopyrightText: Copyright © 2017 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.clientsidefiltering;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientSideFiltering/challenge-store")
public class ShopEndpoint {

  @AllArgsConstructor
  private class CheckoutCodes {

    @Getter private List<CheckoutCode> codes;

    public Optional<CheckoutCode> get(String code) {
      return codes.stream().filter(c -> c.getCode().equals(code)).findFirst();
    }
  }

  @AllArgsConstructor
  @Getter
  private class CheckoutCode {
    private String code;
    private int discount;
  }

  private static final int MAX_COUPON_USES = 1;

  private CheckoutCodes checkoutCodes;
  private final Map<String, AtomicInteger> couponUsageCount = new ConcurrentHashMap<>();

  public ShopEndpoint() {
    List<CheckoutCode> codes = Lists.newArrayList();
    codes.add(new CheckoutCode("webgoat", 25));
    codes.add(new CheckoutCode("owasp", 25));
    codes.add(new CheckoutCode("owasp-webgoat", 50));
    this.checkoutCodes = new CheckoutCodes(codes);
  }

  @GetMapping(value = "/coupons/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  public CheckoutCode getDiscountCode(@PathVariable String code) {
    // Enforce coupon usage limit: each code can only be redeemed a limited number of times
    AtomicInteger usage = couponUsageCount.computeIfAbsent(code, k -> new AtomicInteger(0));
    if (usage.get() >= MAX_COUPON_USES) {
      return new CheckoutCode(code, 0);
    }
    CheckoutCode result;
    if (ClientSideFilteringFreeAssignment.SUPER_COUPON_CODE.equals(code)) {
      result = new CheckoutCode(ClientSideFilteringFreeAssignment.SUPER_COUPON_CODE, 100);
    } else {
      result = checkoutCodes.get(code).orElse(new CheckoutCode("no", 0));
    }
    if (result.getDiscount() > 0) {
      usage.incrementAndGet();
    }
    return result;
  }

  @GetMapping(value = "/coupons", produces = MediaType.APPLICATION_JSON_VALUE)
  public CheckoutCodes all() {
    List<CheckoutCode> all = Lists.newArrayList();
    all.addAll(this.checkoutCodes.getCodes());
    all.add(new CheckoutCode(ClientSideFilteringFreeAssignment.SUPER_COUPON_CODE, 100));
    return new CheckoutCodes(all);
  }
}
