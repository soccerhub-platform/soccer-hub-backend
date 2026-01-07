INSERT INTO public.app_role (code, description) VALUES
                                             ('ADMIN', 'Администратор филиала'),
                                             ('LEAD', 'Лид-клиент, потенциальный клиент'),
                                             ('COACH', 'Тренер спортивной школы'),
                                             ('DISPATCHER', 'Диспетчер, который распределяет заявки'),
                                             ('SUPER_ADMIN', 'Суперадминистратор системы');

-- Clubs
INSERT INTO public.clubs (id, name, slug, email, phone, website, logo_url, address, timezone, is_active, created_at, updated_at, created_by, modified_by)
VALUES
    ('d9fd9832-51a0-492b-9acc-a6d0eb142337', 'FC Kairat', 'kairat', 'kairat@fc.com', '7774882327', null, null, 'Abay 132', 'Asia/Almaty', true, '2025-12-20 23:27:18.901815', '2025-12-20 23:27:18.901815', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a'),
    ('11111111-1111-1111-1111-111111111111', 'SoccerHub', 'soccerhub', 'info@soccerhub.kz', '+77001234567', 'https://soccerhub.kz', 'https://cdn.soccerhub.kz/logo.png', 'Almaty, Abay ave 25', 'Asia/Almaty', true, '2025-12-20 23:25:55.071865', '2025-12-20 23:25:55.071865', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a');

-- Branches
INSERT INTO public.branches (id, name, address, active, club_id, created_at, updated_at, created_by, modified_by)
VALUES
    ('8c99cac4-b44a-465f-9902-4d425a3d01ab', 'Main Branch', 'Abay 123', true, 'd9fd9832-51a0-492b-9acc-a6d0eb142337', '2025-12-20 23:27:54.893752', '2025-12-20 23:27:54.893752', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a'),
    ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'SoccerHub – Almaty East', 'Almaty, Raiymbek 12', true, '11111111-1111-1111-1111-111111111111', '2025-12-20 23:25:55.071865', '2025-12-20 23:25:55.071865', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a'),
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'SoccerHub – Almaty Center', 'Almaty, Tole bi 45', true, '11111111-1111-1111-1111-111111111111', '2025-12-20 23:25:55.071865', '2025-12-20 23:25:55.071865', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a');

INSERT INTO public.app_user (id, email, password_hash, created_by, modified_by)
VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'arsen.gizatov@gmail.com', '$2y$10$7HIJS2n3MQnfBYI9CtKelO9cFCsQcwT7KOtEe3Mdfnk3v7c5ulcay', 'arsen', 'arsen');
INSERT INTO public.app_user (id, email, password_hash, enabled, force_password_change, created_at, updated_at, created_by, modified_by)
VALUES
    ('1bf8334c-0067-4696-b643-e32e8393b2ea', 'admin@fc.com', '$2a$10$C9VAP8ujkn8emo3URrL7Qe6NRa64I8SuMKfVL6AxhCJ5CM7dyAHbq', true, false, '2025-12-20 23:30:14.446351', '2025-12-26 15:30:37.915775', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', '1bf8334c-0067-4696-b643-e32e8393b2ea');


INSERT INTO public.app_user_role (user_id, role_code)
VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'DISPATCHER'),
    ('1bf8334c-0067-4696-b643-e32e8393b2ea', 'ADMIN');

INSERT INTO public.dispatcher_profiles (id, first_name, last_name, phone)
VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'Arsen', 'Gizatov', '+77001234567');

INSERT INTO public.admin_profiles (id, first_name, last_name, email, phone, active, created_at, updated_at, created_by, modified_by)
VALUES
    ('1bf8334c-0067-4696-b643-e32e8393b2ea', 'Arsen', 'Gizatov', 'admin@fc.com', '877758988912', true, '2025-12-20 23:30:14.453588', '2025-12-20 23:30:14.453588', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a');

INSERT INTO public.dispatcher_club (dispatcher_id, club_id)
VALUES
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'd9fd9832-51a0-492b-9acc-a6d0eb142337'),
    ('b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', '11111111-1111-1111-1111-111111111111');

INSERT INTO public.admin_branches (id, admin_id, branch_id, created_at, updated_at, created_by, modified_by)
VALUES
    ('eeae1fd5-39ef-4795-86fe-8b2b9aa796fc', '1bf8334c-0067-4696-b643-e32e8393b2ea', '8c99cac4-b44a-465f-9902-4d425a3d01ab', '2026-01-06 11:43:51.688243', '2026-01-06 11:43:51.688243', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a', 'b4f7c10d-1ef4-428d-ac06-3cb76d26f10a');
