INSERT INTO app_user (name, email, role) VALUES
    ('Ana Popescu',     'ana@example.com',     'MEMBER'),
    ('Mihai Ionescu',   'mihai@example.com',   'MEMBER'),
    ('Elena Dumitru',   'elena@example.com',   'LEAD'),
    ('Radu Stan',       'radu@example.com',    'FACILITY');

INSERT INTO device (name, type, os, asset_tag) VALUES
    ('iPhone 15',            'PHONE',  'iOS 18',            'DP-001'),
    ('iPhone SE (2nd gen)',  'PHONE',  'iOS 16',            'DP-002'),
    ('Pixel 8',              'PHONE',  'Android 15',        'DP-003'),
    ('Samsung Galaxy S21',   'PHONE',  'Android 13',        'DP-004'),
    ('Xiaomi Redmi Note 10', 'PHONE',  'Android 11',        'DP-005'),
    ('iPad Air (5th gen)',   'TABLET', 'iPadOS 18',         'DP-006'),
    ('Galaxy Tab S7',        'TABLET', 'Android 13',        'DP-007'),
    ('ThinkPad T440',        'LAPTOP', 'Windows 7 / IE 11', 'DP-008'),
    ('MacBook Air 2015',     'LAPTOP', 'macOS 10.13 / Safari 11', 'DP-009');
