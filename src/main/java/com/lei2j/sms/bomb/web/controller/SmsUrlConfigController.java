package com.lei2j.sms.bomb.web.controller;

import com.lei2j.sms.bomb.base.entity.Pager;
import com.lei2j.sms.bomb.entity.SmsUrlConfig;
import com.lei2j.sms.bomb.repository.SmsUrlConfigRepository;
import com.lei2j.sms.bomb.util.StringPropertyMatcher;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * @author leijinjun
 * @date 2021/1/16
 **/
@RestController
@RequestMapping("/url/config")
public class SmsUrlConfigController {

    private final SmsUrlConfigRepository smsUrlConfigRepository;

    @Autowired
    public SmsUrlConfigController(SmsUrlConfigRepository smsUrlConfigRepository) {
        this.smsUrlConfigRepository = smsUrlConfigRepository;
    }

    @GetMapping("/page")
    public ResponseEntity<Object> urlConfig(SmsUrlConfig params,
                            @RequestParam(value = "pageNo",required = false,defaultValue = "1")Integer pageNo,
                            @RequestParam(value = "pageSize",required = false,defaultValue = "20")Integer pageSize){
        PageRequest pageRequest = PageRequest.of(Math.max(pageNo - 1, 0), pageSize, Sort.by("id").descending());
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withMatcher("websiteName", StringPropertyMatcher.ofIgnoreStringEmpty());
        Example<SmsUrlConfig> example = Example.of(params, exampleMatcher);
        Page<SmsUrlConfig> page = smsUrlConfigRepository.findAll(example, pageRequest);
        return ResponseEntity.ok(Pager.convert(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmsUrlConfig> get(@PathVariable("id")Integer id){
        return ResponseEntity.of(smsUrlConfigRepository.findById(id));
    }

    @PostMapping("/updateStatus")
    public ResponseEntity<Void> update(Integer id,
                                       @RequestParam(value = "normal") Boolean normal) {
        return smsUrlConfigRepository.findById(id).map(c -> {
            c.setNormal(normal);
            smsUrlConfigRepository.saveAndFlush(c);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    public ResponseEntity<Void> update(SmsUrlConfig smsUrlConfig) {
        return Optional.of(smsUrlConfig)
                .map(c -> {
                    Integer id = c.getId();
                    boolean isCreated = Objects.isNull(id);
                    Optional<SmsUrlConfig> configOptional = isCreated ? Optional.of(c) : smsUrlConfigRepository.findById(id);
                    return configOptional.map(t -> {
                        BeanUtils.copyProperties(c, t, "createAt", "updateAt", "lastUsedTime","normal");
                        if (isCreated) {
                            t.setNormal(true);
                        }
                        if (Objects.isNull(t.getCreateAt())) {
                            t.setCreateAt(LocalDateTime.now());
                        }
                        t.setUpdateAt(LocalDateTime.now());
                        smsUrlConfigRepository.save(t);
                        return ResponseEntity.ok().<Void>build();
                    }).orElse(ResponseEntity.notFound().build());
                }).get();
    }
}
