-- V5: Realistic seed data for Spice Route restaurant chain
-- 1 restaurant · 4 branches · tables · multilingual feedback
-- Only inserted if no feedback rows exist (idempotent via DO block)

DO $$
DECLARE
    v_restaurant_id UUID;
    v_owner_id      UUID;
    v_branch_gulshan  UUID;
    v_branch_dhanmondi UUID;
    v_branch_uttara   UUID;
    v_branch_mirpur   UUID;
    v_manager1_id UUID;
    v_manager2_id UUID;
    -- table ids
    tg1 UUID; tg2 UUID; tg3 UUID; tg4 UUID; tg5 UUID;
    td1 UUID; td2 UUID; td3 UUID; td4 UUID;
    tu1 UUID; tu2 UUID; tu3 UUID;
    tm1 UUID; tm2 UUID; tm3 UUID;
BEGIN
    -- Skip if seed already applied
    IF EXISTS (SELECT 1 FROM restaurants WHERE slug = 'spice-route-demo') THEN
        RETURN;
    END IF;

    -- ── Restaurant & Owner ────────────────────────────────────────────────
    INSERT INTO restaurants (id, name, slug, status)
    VALUES (gen_random_uuid(), 'Spice Route Demo', 'spice-route-demo', 'ACTIVE')
    RETURNING id INTO v_restaurant_id;

    INSERT INTO users (id, name, email, password_hash, role, restaurant_id, is_active)
    VALUES (gen_random_uuid(),
            'Demo Owner',
            'demo@spiceroute.local',
            -- BCrypt of "Demo123!" — change before using in production
            '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj2tS6Ufxrle',
            'OWNER', v_restaurant_id, TRUE)
    RETURNING id INTO v_owner_id;

    UPDATE restaurants SET owner_id = v_owner_id WHERE id = v_restaurant_id;

    -- ── Managers ──────────────────────────────────────────────────────────
    INSERT INTO users (id, name, email, password_hash, role, restaurant_id, is_active)
    VALUES (gen_random_uuid(), 'Rahim Ahmed', 'rahim@spiceroute.local',
            '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj2tS6Ufxrle',
            'MANAGER', v_restaurant_id, TRUE)
    RETURNING id INTO v_manager1_id;

    INSERT INTO users (id, name, email, password_hash, role, restaurant_id, is_active)
    VALUES (gen_random_uuid(), 'Nadia Islam', 'nadia@spiceroute.local',
            '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj2tS6Ufxrle',
            'MANAGER', v_restaurant_id, TRUE)
    RETURNING id INTO v_manager2_id;

    -- ── Branches ──────────────────────────────────────────────────────────
    INSERT INTO branches (id, restaurant_id, name, address, is_active)
    VALUES (gen_random_uuid(), v_restaurant_id, 'Gulshan Branch',
            'House 12, Road 83, Gulshan 2, Dhaka 1212', TRUE)
    RETURNING id INTO v_branch_gulshan;

    INSERT INTO branches (id, restaurant_id, name, address, is_active)
    VALUES (gen_random_uuid(), v_restaurant_id, 'Dhanmondi Branch',
            'Road 27, Dhanmondi, Dhaka 1209', TRUE)
    RETURNING id INTO v_branch_dhanmondi;

    INSERT INTO branches (id, restaurant_id, name, address, is_active)
    VALUES (gen_random_uuid(), v_restaurant_id, 'Uttara Branch',
            'Sector 11, Uttara, Dhaka 1230', TRUE)
    RETURNING id INTO v_branch_uttara;

    INSERT INTO branches (id, restaurant_id, name, address, is_active)
    VALUES (gen_random_uuid(), v_restaurant_id, 'Mirpur Branch',
            'Section 10, Mirpur, Dhaka 1216', TRUE)
    RETURNING id INTO v_branch_mirpur;

    -- Manager assignments
    INSERT INTO manager_branches (user_id, branch_id) VALUES
        (v_manager1_id, v_branch_gulshan),
        (v_manager1_id, v_branch_dhanmondi),
        (v_manager2_id, v_branch_uttara),
        (v_manager2_id, v_branch_mirpur);

    -- ── Tables ────────────────────────────────────────────────────────────
    -- Gulshan (5 tables)
    INSERT INTO tables (id, restaurant_id, branch_id, label, qr_token, is_active)
    VALUES
        (gen_random_uuid(), v_restaurant_id, v_branch_gulshan, 'Table 1', 'demo-g1-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_gulshan, 'Table 2', 'demo-g2-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_gulshan, 'Table 3', 'demo-g3-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_gulshan, 'Table 4', 'demo-g4-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_gulshan, 'Table 5', 'demo-g5-'||substr(md5(random()::text),1,14), TRUE);

    -- Dhanmondi (4 tables)
    INSERT INTO tables (id, restaurant_id, branch_id, label, qr_token, is_active)
    VALUES
        (gen_random_uuid(), v_restaurant_id, v_branch_dhanmondi, 'Table 1', 'demo-d1-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_dhanmondi, 'Table 2', 'demo-d2-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_dhanmondi, 'Table 3', 'demo-d3-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_dhanmondi, 'Table 4', 'demo-d4-'||substr(md5(random()::text),1,14), TRUE);

    -- Uttara (3 tables)
    INSERT INTO tables (id, restaurant_id, branch_id, label, qr_token, is_active)
    VALUES
        (gen_random_uuid(), v_restaurant_id, v_branch_uttara, 'Table 1', 'demo-u1-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_uttara, 'Table 2', 'demo-u2-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_uttara, 'Table 3', 'demo-u3-'||substr(md5(random()::text),1,14), TRUE);

    -- Mirpur (3 tables)
    INSERT INTO tables (id, restaurant_id, branch_id, label, qr_token, is_active)
    VALUES
        (gen_random_uuid(), v_restaurant_id, v_branch_mirpur, 'Table 1', 'demo-m1-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_mirpur, 'Table 2', 'demo-m2-'||substr(md5(random()::text),1,14), TRUE),
        (gen_random_uuid(), v_restaurant_id, v_branch_mirpur, 'Table 3', 'demo-m3-'||substr(md5(random()::text),1,14), TRUE);

    -- ── Feedback — Gulshan (mixed, mostly positive) ───────────────────────
    INSERT INTO feedbacks (restaurant_id, branch_id, table_id, rating, comment, is_anonymous, language, created_at)
    SELECT v_restaurant_id, v_branch_gulshan, t.id,
           f.rating, f.comment, f.anon, f.lang,
           now() - (f.days_ago || ' days')::interval
    FROM (VALUES
        (5, 'Food was absolutely amazing! The biryani is the best in Dhaka.', FALSE, 'en', 1),
        (5, 'খাবার অসাধারণ ছিল। সার্ভিস খুব ভালো।', FALSE, 'bn', 2),
        (4, 'Khub bhalo khabar, tobe deri hoise ektu.', FALSE, 'banglish', 3),
        (5, 'Excellent ambiance and outstanding service. Will definitely come back!', FALSE, 'en', 4),
        (3, 'Food was good but the waiting time was too long. Took 45 minutes for our order.', FALSE, 'en', 5),
        (4, 'ভালো ছিল কিন্তু দাম একটু বেশি মনে হলো।', FALSE, 'bn', 6),
        (5, 'Best restaurant in Gulshan! Staff is very friendly and helpful.', FALSE, 'en', 7),
        (2, 'The AC was not working properly, it was very hot inside.', FALSE, 'en', 8),
        (5, 'Darun khabar! Biriyani ta ekdom top class chilo.', FALSE, 'banglish', 9),
        (4, 'সার্ভিস ভালো। খাবারের মান ধারাবাহিক।', FALSE, 'bn', 10),
        (3, 'Decent food but parking is a problem in this area.', FALSE, 'en', 11),
        (5, 'The kebabs were phenomenal. Perfectly spiced and tender.', FALSE, 'en', 12),
        (1, 'Found a hair in my food. Very disappointing and unhygienic.', FALSE, 'en', 13),
        (4, 'Onek sundor environment. Khabar ta o khub bhalo chilo.', FALSE, 'banglish', 14),
        (5, 'খাবার দ্রুত পরিবেশন করা হয়েছে এবং স্বাদ অতুলনীয়।', FALSE, 'bn', 15),
        (3, 'The fish curry was a bit too salty for my taste.', FALSE, 'en', 16),
        (5, 'Superb dining experience! The desserts were heavenly.', FALSE, 'en', 17),
        (4, 'Service ta bhalo chilo, khabar o moto moto thik e chilo.', FALSE, 'banglish', 18),
        (2, 'Waited over an hour for food. Staff seemed overwhelmed.', FALSE, 'en', 19),
        (5, 'অসাধারণ! প্রতিটি আইটেম পারফেক্ট ছিল।', FALSE, 'bn', 20)
    ) AS f(rating, comment, anon, lang, days_ago)
    CROSS JOIN (SELECT id FROM tables WHERE branch_id = v_branch_gulshan ORDER BY label LIMIT 1) t;

    -- ── Feedback — Dhanmondi (service complaints recurring) ───────────────
    INSERT INTO feedbacks (restaurant_id, branch_id, table_id, rating, comment, is_anonymous, language, created_at)
    SELECT v_restaurant_id, v_branch_dhanmondi, t.id,
           f.rating, f.comment, f.anon, f.lang,
           now() - (f.days_ago || ' days')::interval
    FROM (VALUES
        (3, 'Food is good but service is really slow. We waited 50 minutes.', FALSE, 'en', 1),
        (4, 'ভালো খাবার। তবে ওয়েটারদের আরও মনোযোগী হওয়া উচিত।', FALSE, 'bn', 2),
        (5, 'Excellent mutton rezala! The best I have ever had.', FALSE, 'en', 3),
        (2, 'Service was terrible. Staff was rude and ignored us for 20 minutes.', FALSE, 'en', 4),
        (4, 'Khabar ta darun chilo but service slow chilo onek.', FALSE, 'banglish', 5),
        (5, 'খুব সুন্দর পরিবেশ। পরিবার নিয়ে আসার জন্য পারফেক্ট।', FALSE, 'bn', 6),
        (3, 'The restaurant was understaffed. Had to ask multiple times for water.', FALSE, 'en', 7),
        (5, 'Amazing food quality! The portions are generous too.', FALSE, 'en', 8),
        (2, 'Service deri hoise onek. Staff respond kortase na.', FALSE, 'banglish', 9),
        (4, 'খাবারের স্বাদ ভালো ছিল। দাম যৌক্তিক।', FALSE, 'bn', 10),
        (3, 'Waited a long time, but the food was worth it in the end.', FALSE, 'en', 11),
        (5, 'Superb halim! Will order every time I visit.', FALSE, 'en', 12),
        (1, 'Staff completely ignored our table. Had to leave without ordering.', FALSE, 'en', 13),
        (4, 'Onek bhalo khabar. Service ektu slow but quality good.', FALSE, 'banglish', 14),
        (5, 'সেরা রেস্তোরাঁ! প্রতিবার আসি ভালো লাগে।', FALSE, 'bn', 15)
    ) AS f(rating, comment, anon, lang, days_ago)
    CROSS JOIN (SELECT id FROM tables WHERE branch_id = v_branch_dhanmondi ORDER BY label LIMIT 1) t;

    -- ── Feedback — Uttara (mostly positive with AC/noise complaints) ──────
    INSERT INTO feedbacks (restaurant_id, branch_id, table_id, rating, comment, is_anonymous, language, created_at)
    SELECT v_restaurant_id, v_branch_uttara, t.id,
           f.rating, f.comment, f.anon, f.lang,
           now() - (f.days_ago || ' days')::interval
    FROM (VALUES
        (5, 'Great food and fast service. This branch is well managed.', FALSE, 'en', 1),
        (4, 'ভালো খাবার। তবে একটু বেশি শব্দ ছিল।', FALSE, 'bn', 2),
        (5, 'Bhalo chilo. Staff very helpful and friendly.', FALSE, 'banglish', 3),
        (3, 'The music was too loud. Could not have a conversation.', FALSE, 'en', 4),
        (5, 'খাবারের মান অনেক ভালো। পরিষ্কার পরিচ্ছন্ন।', FALSE, 'bn', 5),
        (4, 'Good food but AC was a bit too cold near the window seats.', FALSE, 'en', 6),
        (5, 'Darun experience! Ei branch e abar ashbo.', FALSE, 'banglish', 7),
        (3, 'Food is average. Nothing special compared to other branches.', FALSE, 'en', 8),
        (5, 'অসাধারণ সার্ভিস। স্টাফ খুব ভদ্র।', FALSE, 'bn', 9),
        (4, 'Nice ambiance, good food. The dessert menu needs more variety.', FALSE, 'en', 10),
        (5, 'Khub sundor jaga. Khabar o darun.', FALSE, 'banglish', 11),
        (2, 'The noise level was unbearable. Sat next to a large group.', FALSE, 'en', 12)
    ) AS f(rating, comment, anon, lang, days_ago)
    CROSS JOIN (SELECT id FROM tables WHERE branch_id = v_branch_uttara ORDER BY label LIMIT 1) t;

    -- ── Feedback — Mirpur (mixed quality, price complaints) ───────────────
    INSERT INTO feedbacks (restaurant_id, branch_id, table_id, rating, comment, is_anonymous, language, created_at)
    SELECT v_restaurant_id, v_branch_mirpur, t.id,
           f.rating, f.comment, f.anon, f.lang,
           now() - (f.days_ago || ' days')::interval
    FROM (VALUES
        (3, 'Food was okay but overpriced for this area.', FALSE, 'en', 1),
        (4, 'ভালো খাবার। তবে দাম একটু বেশি।', FALSE, 'bn', 2),
        (5, 'Darun biryani! Price ta thik ache.', FALSE, 'banglish', 3),
        (2, 'Too expensive. Same food available cheaper nearby.', FALSE, 'en', 4),
        (4, 'পরিষ্কার রেস্তোরাঁ। খাবার ভালো।', FALSE, 'bn', 5),
        (3, 'Quality has gone down recently. Not worth the price anymore.', FALSE, 'en', 6),
        (5, 'Excellent food as always! Worth every taka.', FALSE, 'en', 7),
        (2, 'Dam onek beshi. Quality o age theke kome geche.', FALSE, 'banglish', 8),
        (4, 'ভালো পরিবেশ ও ভালো খাবার।', FALSE, 'bn', 9),
        (3, 'The menu prices increased but quality stayed the same.', FALSE, 'en', 10),
        (5, 'Bhalo khabar, bhalo service. Khushi holam.', FALSE, 'banglish', 11),
        (1, 'Got food poisoning after eating here. Very bad experience.', FALSE, 'en', 12),
        (4, 'সার্ভিস দ্রুত ছিল। খাবার ভালো ছিল।', FALSE, 'bn', 13)
    ) AS f(rating, comment, anon, lang, days_ago)
    CROSS JOIN (SELECT id FROM tables WHERE branch_id = v_branch_mirpur ORDER BY label LIMIT 1) t;

END $$;
