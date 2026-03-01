# Entity Implementation Guide

This document outlines the standard procedure for creating new entities and their corresponding data access layers in the microservices framework.

## 1. Base Framework Components

Our framework provides base entities to standardize fields and behaviors across all tables.

### 1.1 Base Entities

| Base Class | Purpose | Included Fields |
|---|---|---|
| `AuditEntity` | Basic entity requiring tracking of creation and updates | `createdAt`, [createdBy](file:///c:/Project/standard-microservice/business-service/src/main/java/com/enterprise/business/repository/base/BaseSpecifications.java#25-28), `updatedAt`, [updatedBy](file:///c:/Project/standard-microservice/business-service/src/main/java/com/enterprise/business/repository/base/BaseSpecifications.java#55-58) |
| `SoftDeleteEntity` | Entities that should never be hard-deleted | Inherits `AuditEntity` + `deleted`, `deletedAt`, `deletedBy`, `version` |
| `StatefulEntity<S,H>` | Entities that progress through a state machine | Inherits `SoftDeleteEntity` + `status` |
| `HistoryEntity` | To store snapshot histories of `StatefulEntity` | Inherits `AuditEntity` + `action`, `previousStatus`, `currentSnapshot`, `diff`, `changedBy`, `correlationId` |

## 2. Creating a Standard Entity

When building an entity, decide whether it requires hard deletion or soft deletion, and whether it requires state tracking.

### 2.1 Entity Definition

For most persistent business data, use `SoftDeleteEntity`:

```java
import com.enterprise.business.entity.base.SoftDeleteEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "products")
@Getter
@Setter
// IMPORTANT: SQLRestriction filters out deleted records by default at the Hibernate level
@SQLRestriction("deleted = false")
public class Product extends SoftDeleteEntity {
    
    // id is already defined in SoftDeleteEntity
    
    private String name;
    private String description;
}
```

### 2.2 Repository Implementation

Repositories for `SoftDeleteEntity` should extend [SoftDeleteRepository](file:///c:/Project/standard-microservice/business-service/src/main/java/com/enterprise/business/repository/base/SoftDeleteRepository.java#22-58):

```java
import com.enterprise.business.repository.base.SoftDeleteRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends SoftDeleteRepository<Product, UUID> {
    // Custom query methods here
    // Note: You do not need to filter "deleted = false" as @SQLRestriction handles it
}
```

## 3. Implementing Complex Relationships (Many-to-Many)

When dealing with Many-to-Many (`@ManyToMany`) relationships, care must be taken to ensure performance, avoid lazy loading exceptions, and maintain clean database schemas.

### 3.1 Scenario: `Student` and `Course`

A student can enroll in multiple courses, and a course can have multiple students.

#### Step 1: Decide on the Owning Side
Always choose one entity to be the "owning" side. This entity will manage the `@JoinTable`. The other entity will be the "inverse" side using `mappedBy`.

#### Step 2: The Owning Side (`Student.java`)

```java
@Entity
@Getter
@Setter
@Table(name = "students")
@SQLRestriction("deleted = false")
public class Student extends SoftDeleteEntity {
    
    private String name;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();

    // Utility methods to maintain both sides of the relationship
    public void addCourse(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }

    public void removeCourse(Course course) {
        courses.remove(course);
        course.getStudents().remove(this);
    }
}
```

#### Step 3: The Inverse Side (`Course.java`)

```java
@Entity
@Getter
@Setter
@Table(name = "courses")
@SQLRestriction("deleted = false")
public class Course extends SoftDeleteEntity {
    
    private String title;

    @ManyToMany(mappedBy = "courses")
    // Use @JsonIgnore if returning this entity in an API to prevent infinite recursion
    private Set<Student> students = new HashSet<>();
}
```

### 3.2 Many-to-Many with Extra Columns (Recommended for Soft Delete)

If your relationship needs fields like `enrolledAt` or requires soft-deletion for the relationship itself, **DO NOT** use `@ManyToMany`. Instead, use an explicit mapping entity with `@ManyToOne`.

#### Concept:
- `Student` (1 -> N) `StudentCourse` (N <- 1) `Course`

```java
@Entity
@Table(name = "student_courses")
@Getter
@Setter
@SQLRestriction("deleted = false")
public class StudentCourse extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    
    private String role; // Extra column
}
```

This ensures the relationship itself benefits from auditing and soft-delete behaviors inherited from the Base Entities.

## 4. Best Practices
1. **Never use `@Data` or `@EqualsAndHashCode` on JPA Entities**: They cause performance issues and stack overflows with bidirectional relationships. Use `@Getter` and `@Setter`.
2. **Use Sets instead of Lists for `@ManyToMany`**: This prevents Hibernate from deleting and re-inserting all rows when updating the collection.
3. **Always use helper methods**: When adding/removing from a bidirectional relationship, update both sides in memory (e.g., `addCourse()` method above).
