package com.collaboffice.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties("action")
@Document(indexName = "task_index", createIndex = false)
public class TaskIndex {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Long)
    private Long spaceId;
    
    @Field(type = FieldType.Long)
    private Long assigneeId;
    
    @Field(type = FieldType.Integer)
    private Integer status;
}
