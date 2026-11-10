package com.collaboffice.user.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.collaboffice.user.entity.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class RoleEntityMappingTest {
    Set<String> columns(Class<?> entity){return TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"role-schema"),entity).getFieldList().stream().map(f->f.getColumn()).collect(Collectors.toSet());}
    @Test void roleColumnsMatchV1Schema(){assertEquals(Set.of("role_name","role_code","role_sort","status","remark","create_by","create_time","update_time","deleted"),columns(SysRole.class));}
    @Test void permissionColumnsMatchV1Schema(){assertEquals(Set.of("parent_id","permission_name","permission_code","type","icon","sort","status","create_time"),columns(SysPermission.class));}
}
