<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

class ApprovePendingHrAdminDocuments extends Migration
{
    /**
     * Auto-approve all pending documents that were created by HR or Admin users.
     *
     * Before this fix, HR/Admin documents were stored as 'pending' by default,
     * making them invisible to employees. The new store() logic auto-approves them,
     * but existing records need a one-time backfill.
     */
    public function up()
    {
        DB::statement("
            UPDATE documents
            SET status      = 'approved',
                reviewed_at = NOW()
            WHERE status     = 'pending'
              AND is_active  = true
              AND created_by IN (
                  SELECT ur.user_id
                  FROM user_roles ur
                  JOIN roles r ON ur.role_id = r.id
                  WHERE r.name IN ('admin', 'hr')
              )
        ");
    }

    public function down()
    {
        // Cannot reliably reverse — we don't know which rows were changed by this migration
    }
}
