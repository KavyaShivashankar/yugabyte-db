package com.yugabyte.yw.models.rbac;

import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_ONLY;
import static io.swagger.annotations.ApiModelProperty.AccessMode.READ_WRITE;
import static play.mvc.Http.Status.BAD_REQUEST;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yugabyte.yw.common.PlatformServiceException;
import com.yugabyte.yw.common.rbac.PermissionInfoIdentifier;
import com.yugabyte.yw.models.helpers.PermissionDetails;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.EnumValue;
import io.ebean.annotation.WhenModified;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Slf4j
public class Role extends Model {

  @Id
  @Column(name = "role_uuid", nullable = false)
  @ApiModelProperty(value = "Role UUID", accessMode = READ_ONLY)
  private UUID roleUUID;

  @Column(name = "customer_uuid", nullable = false)
  @ApiModelProperty(value = "Customer UUID")
  private UUID customerUUID;

  @Column(name = "name", nullable = false)
  @ApiModelProperty(value = "Role name", accessMode = READ_WRITE)
  private String name;

  @Column(name = "created_on", nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "Role create time", example = "2022-12-12T13:07:18Z")
  private Date createdOn;

  @Setter
  @Column(name = "updated_on", nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  @ApiModelProperty(value = "Role last updated time", example = "2022-12-12T13:07:18Z")
  @WhenModified
  private Date updatedOn;

  public enum RoleType {
    @EnumValue("System")
    System,

    @EnumValue("Custom")
    Custom
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "role_type", nullable = false)
  @ApiModelProperty(value = "Type of the role")
  private RoleType roleType;

  @Setter
  @DbJson
  @Column(name = "permission_details")
  @ApiModelProperty(value = "Permission details of the role")
  private PermissionDetails permissionDetails;

  public static final Finder<UUID, Role> find = new Finder<UUID, Role>(Role.class) {};

  public void updatePermissionList(Set<PermissionInfoIdentifier> permissionList) {
    this.permissionDetails.setPermissionList(permissionList);
    this.setUpdatedOn(new Date());
    this.update();
  }

  public static Role create(
      UUID customerUUID,
      String name,
      RoleType roleType,
      Set<PermissionInfoIdentifier> permissionList) {
    Role role =
        new Role(
            UUID.randomUUID(),
            customerUUID,
            name,
            new Date(),
            new Date(),
            roleType,
            new PermissionDetails(permissionList));
    role.save();
    return role;
  }

  public static Role get(UUID roleUUID) {
    return find.query().where().eq("role_uuid", roleUUID).findOne();
  }

  public static Role getOrBadRequest(UUID roleUUID) throws PlatformServiceException {
    Role role = get(roleUUID);
    if (role == null) {
      throw new PlatformServiceException(BAD_REQUEST, "Invalid role UUID: " + roleUUID);
    }
    return role;
  }

  public static Role get(String name) {
    return find.query().where().eq("name", name).findOne();
  }

  public static List<Role> getAll() {
    return find.query().findList();
  }

  public static List<Role> getAll(RoleType roleType) {
    return find.query().where().eq("role_type", roleType.toString()).findList();
  }
}
