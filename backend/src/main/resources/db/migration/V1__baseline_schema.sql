-- Baseline schema: matches the entity model as of commit 2394e93 (previously managed by ddl-auto=update).
-- Existing databases are baselined at version 1 and skip this script.


    create table audit_logs (
        entity_id bigint,
        id bigint identity not null,
        timestamp datetimeoffset(6) not null,
        action varchar(50) not null,
        entity_type varchar(50) not null,
        ip_address varchar(50),
        verification_hash varchar(64),
        actor_role varchar(100),
        actor_email varchar(150),
        actor_name varchar(150) not null,
        details NVARCHAR(MAX),
        entity_name varchar(255),
        primary key (id)
    );

    create table automation_rules (
        execution_count int not null,
        is_enabled bit not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        last_executed_at datetimeoffset(6),
        project_id bigint not null,
        action_type varchar(50) not null,
        target_role varchar(50),
        trigger_event varchar(50) not null,
        name varchar(150) not null,
        description varchar(500),
        primary key (id)
    );

    create table channel_members (
        channel_id bigint not null,
        user_id uniqueidentifier not null,
        primary key (channel_id, user_id)
    );

    create table channels (
        is_private bit not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        project_id bigint,
        created_by uniqueidentifier,
        type varchar(20) not null,
        icon varchar(50),
        name varchar(80) not null,
        description varchar(255),
        primary key (id)
    );

    create table daily_work_logs (
        completed_tasks_count int not null,
        log_date date not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        user_id uniqueidentifier not null,
        summary_text NVARCHAR(MAX) not null,
        primary key (id)
    );

    create table meetings (
        channel_id bigint,
        ended_at datetimeoffset(6),
        id bigint identity not null,
        project_id bigint,
        started_at datetimeoffset(6) not null,
        host_id uniqueidentifier not null,
        status varchar(20) not null,
        room_id varchar(100) not null,
        title varchar(200) not null,
        primary key (id)
    );

    create table message_seen_receipts (
        channel_id bigint not null,
        id bigint identity not null,
        message_id bigint not null,
        seen_at datetimeoffset(6) not null,
        user_id uniqueidentifier not null,
        primary key (id)
    );

    create table messages (
        is_pinned bit not null,
        channel_id bigint not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        sender_id uniqueidentifier not null,
        attachment_url varchar(500),
        content NVARCHAR(MAX) not null,
        primary key (id)
    );

    create table notifications (
        is_read bit not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        read_at datetimeoffset(6),
        reference_id bigint,
        target_role_id bigint,
        recipient_id uniqueidentifier,
        priority varchar(20) not null,
        sender_avatar varchar(20),
        action_type varchar(50),
        type varchar(50) not null,
        sender_name varchar(100),
        link_url varchar(500),
        message NVARCHAR(MAX) not null,
        title varchar(255) not null,
        primary key (id)
    );

    create table permissions (
        id bigint identity not null,
        action varchar(50) not null,
        module varchar(50) not null,
        description varchar(255),
        primary key (id)
    );

    create table projects (
        allocated_budget float(53),
        due_date date,
        start_date date,
        total_budget float(53),
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        team_id bigint,
        updated_at datetimeoffset(6) not null,
        currency varchar(10),
        manager_id uniqueidentifier,
        status varchar(30) not null,
        name varchar(150) not null,
        description varchar(1000),
        primary key (id)
    );

    create table role_permissions (
        permission_id bigint not null,
        role_id bigint not null,
        primary key (permission_id, role_id)
    );

    create table roles (
        is_system_role bit not null,
        status bit not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        name varchar(50) not null,
        description varchar(255),
        primary key (id)
    );

    create table sprints (
        end_date date,
        start_date date,
        target_story_points int,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        project_id bigint not null,
        status varchar(20) not null,
        name varchar(100) not null,
        goal varchar(500),
        primary key (id)
    );

    create table stages (
        is_done_stage bit not null,
        position int not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        project_id bigint not null,
        name varchar(80) not null,
        primary key (id)
    );

    create table subtasks (
        is_completed bit not null,
        position int not null,
        completed_at datetimeoffset(6),
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        task_id bigint not null,
        completed_by varchar(150),
        title varchar(255) not null,
        primary key (id)
    );

    create table task_attachments (
        id bigint identity not null,
        task_id bigint not null,
        uploaded_at datetimeoffset(6) not null,
        file_size varchar(50),
        file_type varchar(50) not null,
        uploaded_by varchar(150) not null,
        file_name varchar(255) not null,
        file_url NVARCHAR(MAX) not null,
        primary key (id)
    );

    create table task_comments (
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        task_id bigint not null,
        author_id uniqueidentifier not null,
        attachment_url varchar(500),
        content NVARCHAR(MAX) not null,
        primary key (id)
    );

    create table task_dependencies (
        depends_on_task_id bigint not null,
        task_id bigint not null,
        primary key (depends_on_task_id, task_id)
    );

    create table task_dependency_links (
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        source_task_id bigint not null,
        target_task_id bigint not null,
        dependency_type varchar(30) not null,
        created_by varchar(100),
        target_task_title varchar(255),
        primary key (id)
    );

    create table task_time_logs (
        hours_spent float(53) not null,
        logged_date date not null,
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        task_id bigint not null,
        user_id uniqueidentifier not null,
        description varchar(500),
        primary key (id)
    );

    create table tasks (
        estimated_hours float(53),
        is_archived bit not null,
        is_blocked bit not null,
        progress_percentage int not null,
        story_points int,
        total_hours_spent float(53),
        approved_at datetimeoffset(6),
        archived_at datetimeoffset(6),
        blocked_at datetimeoffset(6),
        created_at datetimeoffset(6) not null,
        deadline datetimeoffset(6),
        id bigint identity not null,
        project_id bigint not null,
        sprint_id bigint,
        stage_id bigint,
        start_date datetimeoffset(6),
        updated_at datetimeoffset(6) not null,
        approved_by uniqueidentifier,
        assignee_id uniqueidentifier,
        reporter_id uniqueidentifier,
        priority varchar(20) not null,
        git_commit_hash varchar(64),
        archived_by varchar(100),
        git_branch varchar(150),
        git_pr_url varchar(500),
        blocker_reason varchar(1000),
        description NVARCHAR(MAX),
        title varchar(255) not null,
        primary key (id)
    );

    create table team_members (
        team_id bigint not null,
        user_id uniqueidentifier not null,
        primary key (team_id, user_id)
    );

    create table teams (
        created_at datetimeoffset(6) not null,
        id bigint identity not null,
        lead_id uniqueidentifier,
        name varchar(100) not null,
        description varchar(500),
        primary key (id)
    );

    create table user_permission_overrides (
        is_granted bit not null,
        id bigint identity not null,
        permission_id bigint not null,
        user_id uniqueidentifier not null,
        primary key (id)
    );

    create table users (
        on_leave bit not null,
        created_at datetimeoffset(6) not null,
        role_id bigint,
        updated_at datetimeoffset(6) not null,
        id uniqueidentifier not null,
        status varchar(20) not null,
        full_name varchar(100) not null,
        avatar_url varchar(500),
        fcm_token varchar(500),
        email varchar(255) not null,
        leave_note varchar(255),
        password_hash varchar(255) not null,
        primary key (id)
    );

    create table webhook_subscriptions (
        delivery_count int,
        is_enabled bit,
        created_at datetimeoffset(6),
        id bigint identity not null,
        last_delivered_at datetimeoffset(6),
        project_id bigint not null,
        last_response_status varchar(50),
        name varchar(150) not null,
        secret_token varchar(200),
        events varchar(500),
        target_url varchar(500) not null,
        primary key (id)
    );

    alter table daily_work_logs 
       add constraint UKqkid3rubuhe0bdpdawl2j1ahe unique (user_id, log_date);

    alter table meetings 
       add constraint UKkrspvalc39mb8j4lh7tmpdkoe unique (room_id);

    alter table message_seen_receipts 
       add constraint UK65fvcc0qo1uq29ldift0375pi unique (message_id, user_id);

    alter table permissions 
       add constraint UKg6w0msto4usgebkkje1tjujeo unique (module, action);

    alter table roles 
       add constraint UKofx66keruapi6vyqpv6f2or37 unique (name);

    alter table user_permission_overrides 
       add constraint UKhy2rfmgkwvp25v78mv3sllrkp unique (user_id, permission_id);

    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table channel_members 
       add constraint FKjunpxp98pv6g23t5d3w5hx8ji 
       foreign key (user_id) 
       references users;

    alter table channel_members 
       add constraint FK5w7mqibtg5vbjt7xq6xyhc479 
       foreign key (channel_id) 
       references channels;

    alter table channels 
       add constraint FKj5bvg2dapo5aqh7j9xc2wr8e3 
       foreign key (created_by) 
       references users;

    alter table channels 
       add constraint FKa3oqy7jr1s3ruukboxtp2j4c3 
       foreign key (project_id) 
       references projects;

    alter table daily_work_logs 
       add constraint FKl3k18uhvatecrol31eht2v3wj 
       foreign key (user_id) 
       references users;

    alter table meetings 
       add constraint FKfulcdj14tv0d6f1xn5tqhrkvb 
       foreign key (channel_id) 
       references channels;

    alter table meetings 
       add constraint FKsa773v03wufueg9de8cmjydr4 
       foreign key (host_id) 
       references users;

    alter table meetings 
       add constraint FKbt3vmcde0efob60esnfkyaeg0 
       foreign key (project_id) 
       references projects;

    alter table message_seen_receipts 
       add constraint FK60do2e05ybtnaatj6a37g8m52 
       foreign key (channel_id) 
       references channels;

    alter table message_seen_receipts 
       add constraint FKt32mfex03yyu866fntie840sy 
       foreign key (message_id) 
       references messages;

    alter table message_seen_receipts 
       add constraint FKlhictjw1b2duqapj87id4fai0 
       foreign key (user_id) 
       references users;

    alter table messages 
       add constraint FK3u3ckbhwq9se1cmopk2pq05b2 
       foreign key (channel_id) 
       references channels;

    alter table messages 
       add constraint FK4ui4nnwntodh6wjvck53dbk9m 
       foreign key (sender_id) 
       references users;

    alter table notifications 
       add constraint FKqqnsjxlwleyjbxlmm213jaj3f 
       foreign key (recipient_id) 
       references users;

    alter table notifications 
       add constraint FKrdprswg5dkns7upacs9wrt96 
       foreign key (target_role_id) 
       references roles;

    alter table projects 
       add constraint FKurl8wb4qjly2c5xwdcpetuxs 
       foreign key (manager_id) 
       references users;

    alter table projects 
       add constraint FKmqih0928bq6r3gbuh47giq8w 
       foreign key (team_id) 
       references teams;

    alter table role_permissions 
       add constraint FKegdk29eiy7mdtefy5c7eirr6e 
       foreign key (permission_id) 
       references permissions;

    alter table role_permissions 
       add constraint FKn5fotdgk8d1xvo8nav9uv3muc 
       foreign key (role_id) 
       references roles;

    alter table stages 
       add constraint FKpbjd8m2rbtpxdmmf8r7wtspgk 
       foreign key (project_id) 
       references projects;

    alter table subtasks 
       add constraint FKsvs126nsj9ohhvwjog5ddp76x 
       foreign key (task_id) 
       references tasks;

    alter table task_attachments 
       add constraint FK4eyiisq4wyx2mfj3p9h8ppufo 
       foreign key (task_id) 
       references tasks;

    alter table task_comments 
       add constraint FKtbd7uwo21s4shik5f0mghfk21 
       foreign key (author_id) 
       references users;

    alter table task_comments 
       add constraint FK9517viwn2geh1gpivj6l9y64u 
       foreign key (task_id) 
       references tasks;

    alter table task_dependencies 
       add constraint FKn6qipbhgl3vk43xnf30han7mp 
       foreign key (depends_on_task_id) 
       references tasks;

    alter table task_dependencies 
       add constraint FKerlktvi2bud6uauih348u0loj 
       foreign key (task_id) 
       references tasks;

    alter table task_time_logs 
       add constraint FKjw8l4l5mrd64ka7dn7q8janpl 
       foreign key (task_id) 
       references tasks;

    alter table task_time_logs 
       add constraint FK1ck3fyfm5eocjh34jiprc8qkf 
       foreign key (user_id) 
       references users;

    alter table tasks 
       add constraint FKnoi1blfshcbfa7mlp3k4ndrjw 
       foreign key (approved_by) 
       references users;

    alter table tasks 
       add constraint FKekr1dgiqktpyoip3qmp6lxsit 
       foreign key (assignee_id) 
       references users;

    alter table tasks 
       add constraint FKsfhn82y57i3k9uxww1s007acc 
       foreign key (project_id) 
       references projects;

    alter table tasks 
       add constraint FKbvjdsa9y725wovwlq4sjhodyk 
       foreign key (reporter_id) 
       references users;

    alter table tasks 
       add constraint FKk0qqb3jrxpdgjhqcoxilrusxh 
       foreign key (stage_id) 
       references stages;

    alter table team_members 
       add constraint FKee8x7x5026imwmma9kndkxs36 
       foreign key (user_id) 
       references users;

    alter table team_members 
       add constraint FKtgca08el3ofisywcf11f0f76t 
       foreign key (team_id) 
       references teams;

    alter table teams 
       add constraint FK8dwk4qe8sem9o0t4q0cqy1wef 
       foreign key (lead_id) 
       references users;

    alter table user_permission_overrides 
       add constraint FKl41jpmfrwb159194d70o3vw33 
       foreign key (permission_id) 
       references permissions;

    alter table user_permission_overrides 
       add constraint FKkppc8es7a6ro6cey0regbxiit 
       foreign key (user_id) 
       references users;

    alter table users 
       add constraint FKp56c1712k691lhsyewcssf40f 
       foreign key (role_id) 
       references roles;
