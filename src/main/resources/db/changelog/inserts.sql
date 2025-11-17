INSERT INTO app_role (code, description) VALUES
                                             ('ADMIN', 'Администратор филиала'),
                                             ('LEAD', 'Лид-клиент, потенциальный клиент'),
                                             ('COACH', 'Тренер спортивной школы'),
                                             ('DISPATCHER', 'Диспетчер, который распределяет заявки'),
                                             ('SUPER_ADMIN', 'Суперадминистратор системы');

-- Clubs
INSERT INTO clubs (id, name, slug, email, phone, website, logo_url, address, timezone, is_active) VALUES
    ('11111111-1111-1111-1111-111111111111', 'SoccerHub', 'soccerhub', 'info@soccerhub.kz', '+77001234567', 'https://soccerhub.kz',
     'https://cdn.soccerhub.kz/logo.png', 'Almaty, Abay ave 25', 'Asia/Almaty', true);

-- Branches
INSERT INTO branches (id, name, address, club_id, active) VALUES
                                                              ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'SoccerHub – Almaty Center', 'Almaty, Tole bi 45', '11111111-1111-1111-1111-111111111111', true),
                                                              ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'SoccerHub – Almaty East', 'Almaty, Raiymbek 12', '11111111-1111-1111-1111-111111111111', true);

INSERT INTO app_user (id, email, password_hash, created_by, modified_by) VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'arsen.gizatov@gmail.com', '$2y$10$7HIJS2n3MQnfBYI9CtKelO9cFCsQcwT7KOtEe3Mdfnk3v7c5ulcay', 'arsen', 'arsen');

INSERT INTO app_user_role (user_id, role_code) VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'DISPATCHER');

INSERT INTO dispatcher_profile(id, first_name, last_name, phone) VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'Arsen', 'Gizatov', '+77001234567');
