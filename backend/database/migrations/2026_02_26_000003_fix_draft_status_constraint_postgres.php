<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

class FixDraftStatusConstraintPostgres extends Migration
{
    public function up()
    {
        $driver = DB::getDriverName();

        if ($driver === 'pgsql') {
            // PostgreSQL: drop the existing check constraint and re-create it with 'draft' included.
            // The constraint name is the one Laravel auto-generates for $table->enum().
            DB::statement("ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check");
            DB::statement("ALTER TABLE documents ADD CONSTRAINT documents_status_check
                CHECK (status IN ('draft', 'pending', 'approved', 'rejected'))");
        } else {
            // MySQL: ALTER the ENUM column directly.
            try {
                DB::statement("ALTER TABLE documents MODIFY COLUMN status
                    ENUM('draft','pending','approved','rejected') NOT NULL DEFAULT 'pending'");
            } catch (\Exception $e) {
                // SQLite does not support MODIFY — safe to skip, SQLite has no enum constraints.
            }
        }
    }

    public function down()
    {
        $driver = DB::getDriverName();

        // Move any existing drafts back to pending before removing the value
        DB::table('documents')->where('status', 'draft')->update(['status' => 'pending']);

        if ($driver === 'pgsql') {
            DB::statement("ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check");
            DB::statement("ALTER TABLE documents ADD CONSTRAINT documents_status_check
                CHECK (status IN ('pending', 'approved', 'rejected'))");
        } else {
            try {
                DB::statement("ALTER TABLE documents MODIFY COLUMN status
                    ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending'");
            } catch (\Exception $e) {
                // ignored on SQLite
            }
        }
    }
}
