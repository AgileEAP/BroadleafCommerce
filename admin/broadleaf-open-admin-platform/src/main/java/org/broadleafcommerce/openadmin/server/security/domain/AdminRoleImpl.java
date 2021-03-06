/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.openadmin.server.security.domain;

import org.broadleafcommerce.common.presentation.client.VisibilityEnum;
import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author jfischer
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BLC_ADMIN_ROLE")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
@AdminPresentationClass(friendlyName = "AdminRoleImpl_baseAdminRole")
public class AdminRoleImpl implements AdminRole {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "AdminRoleId", strategy = GenerationType.TABLE)
    @TableGenerator(name = "AdminRoleId", table = "SEQUENCE_GENERATOR", pkColumnName = "ID_NAME", valueColumnName = "ID_VAL", pkColumnValue = "AdminRoleImpl", allocationSize = 50)
    @Column(name = "ADMIN_ROLE_ID")
    @AdminPresentation(friendlyName = "AdminRoleImpl_Admin_Role_ID", group = "AdminRoleImpl_Primary_Key", visibility = VisibilityEnum.HIDDEN_ALL)
    protected Long id;

    @Column(name = "NAME", nullable=false, unique = true)
    @AdminPresentation(friendlyName = "AdminRoleImpl_Name", order=1, group = "AdminRoleImpl_Role", prominent=true)
    protected String name;

    @Column(name = "DESCRIPTION", nullable=false)
    @AdminPresentation(friendlyName = "AdminRoleImpl_Description", order=2, group = "AdminRoleImpl_Role", prominent=true)
    protected String description;

    /** All users that have this role */
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminUserImpl.class)
    @JoinTable(name = "BLC_ADMIN_USER_ROLE_XREF", joinColumns = @JoinColumn(name = "ADMIN_ROLE_ID", referencedColumnName = "ADMIN_ROLE_ID"), inverseJoinColumns = @JoinColumn(name = "ADMIN_USER_ID", referencedColumnName = "ADMIN_USER_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected Set<AdminUser> allUsers = new HashSet<AdminUser>();

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = AdminPermissionImpl.class)
    @JoinTable(name = "BLC_ADMIN_ROLE_PERMISSION_XREF", joinColumns = @JoinColumn(name = "ADMIN_ROLE_ID", referencedColumnName = "ADMIN_ROLE_ID"), inverseJoinColumns = @JoinColumn(name = "ADMIN_PERMISSION_ID", referencedColumnName = "ADMIN_PERMISSION_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected Set<AdminPermission> allPermissions= new HashSet<AdminPermission>();


    public Set<AdminPermission> getAllPermissions() {
        return allPermissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Set<AdminUser> getAllUsers() {
        return allUsers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAllPermissions(Set<AdminPermission> allPermissions) {
        this.allPermissions = allPermissions;
    }

}
