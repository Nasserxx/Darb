-- Map legacy free-text relationship values to ParentRelationship enum names.
UPDATE parent_student
SET relationship = CASE
    WHEN relationship IS NULL OR btrim(relationship) = '' THEN NULL
    WHEN upper(btrim(relationship)) IN (
        'FATHER', 'MOTHER', 'STEPFATHER', 'STEPMOTHER',
        'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
        'BROTHER', 'SISTER', 'GUARDIAN', 'PARENT', 'OTHER'
    ) THEN upper(btrim(relationship))
    WHEN lower(btrim(relationship)) IN ('father', 'vater')
         OR btrim(relationship) IN ('أب') THEN 'FATHER'
    WHEN lower(btrim(relationship)) IN ('mother', 'mutter')
         OR btrim(relationship) IN ('أم') THEN 'MOTHER'
    WHEN lower(btrim(relationship)) IN ('stepfather', 'stiefvater')
         OR btrim(relationship) IN ('زوج الأم') THEN 'STEPFATHER'
    WHEN lower(btrim(relationship)) IN ('stepmother', 'stiefmutter')
         OR btrim(relationship) IN ('زوجة الأب') THEN 'STEPMOTHER'
    WHEN lower(btrim(relationship)) IN ('grandfather', 'großvater', 'grossvater')
         OR btrim(relationship) IN ('جد') THEN 'GRANDFATHER'
    WHEN lower(btrim(relationship)) IN ('grandmother', 'großmutter', 'grossmutter')
         OR btrim(relationship) IN ('جدة') THEN 'GRANDMOTHER'
    WHEN lower(btrim(relationship)) IN ('uncle', 'onkel')
         OR btrim(relationship) IN ('عم', 'خال', 'عم / خال') THEN 'UNCLE'
    WHEN lower(btrim(relationship)) IN ('aunt', 'tante')
         OR btrim(relationship) IN ('عمة', 'خالة', 'عمة / خالة') THEN 'AUNT'
    WHEN lower(btrim(relationship)) IN ('brother', 'bruder')
         OR btrim(relationship) IN ('أخ') THEN 'BROTHER'
    WHEN lower(btrim(relationship)) IN ('sister', 'schwester')
         OR btrim(relationship) IN ('أخت') THEN 'SISTER'
    WHEN lower(btrim(relationship)) IN ('guardian', 'vormund')
         OR btrim(relationship) IN ('ولي أمر') THEN 'GUARDIAN'
    WHEN lower(btrim(relationship)) IN ('parent', 'elternteil')
         OR btrim(relationship) IN ('والد', 'والدة', 'والد/والدة') THEN 'PARENT'
    WHEN lower(btrim(relationship)) IN ('other', 'sonstiges')
         OR btrim(relationship) IN ('أخرى') THEN 'OTHER'
    ELSE 'OTHER'
END;

ALTER TABLE parent_student
    ADD CONSTRAINT chk_parent_student_relationship
    CHECK (relationship IS NULL OR relationship IN (
        'FATHER', 'MOTHER', 'STEPFATHER', 'STEPMOTHER',
        'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
        'BROTHER', 'SISTER', 'GUARDIAN', 'PARENT', 'OTHER'
    ));
