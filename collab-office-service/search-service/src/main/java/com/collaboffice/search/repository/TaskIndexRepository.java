package com.collaboffice.search.repository;

import com.collaboffice.search.entity.TaskIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface TaskIndexRepository extends ElasticsearchRepository<TaskIndex, String> {
    List<TaskIndex> findByTitleContainingOrDescriptionContaining(String titleKeyword, String descKeyword);
}
