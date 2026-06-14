INSERT INTO public.app_role (code, description)
SELECT 'CLIENT', 'Клиент школы'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.app_role
    WHERE code = 'CLIENT'
);
