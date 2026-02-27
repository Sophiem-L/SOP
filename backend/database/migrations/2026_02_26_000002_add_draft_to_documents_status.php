<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

class AddDraftToDocumentsStatus extends Migration
{
    /**
     * Alters the status column to include 'draft'.
     * Handles both PostgreSQL (CHECK constraint) and MySQL (ENUM).
     */
    public function up()
    {
        $driver = DB::getDriverName();

        if ($driver === 'pgsql') {
            DB::statement("ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check");
            DB::statement("ALTER TABLE documents ADD CONSTRAINT documents_status_check
                CHECK (status IN ('draft', 'pending', 'approved', 'rejected'))");
        } else {
            try {
                DB::statement("ALTER TABLE documents MODIFY COLUMN status
                    ENUM('draft','pending','approved','rejected') NOT NULL DEFAULT 'pending'");
            } catch (\Exception $e) {
                // SQLite does not support MODIFY — safe to skip
            }
        }
    }

    public function down()
    {
        $driver = DB::getDriverName();

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
