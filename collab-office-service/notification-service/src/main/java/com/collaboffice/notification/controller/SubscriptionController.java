package com.collaboffice.notification.controller;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.notification.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/notification/subscriptions") @RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptions;
    @GetMapping public CommonResult<List<Map<String,Object>>> list(){return CommonResult.success(subscriptions.list());}
    @PostMapping public CommonResult<Void> save(@RequestBody Map<String,String> body){subscriptions.save(body);return CommonResult.success(null);}
    @DeleteMapping("/{id}") public CommonResult<Void> remove(@PathVariable Long id){subscriptions.remove(id);return CommonResult.success(null);}
}
