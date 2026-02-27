<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

class RemoveSampleArticles extends Migration
{
    /**
     * Delete any system-seeded/sample articles that were not created by real users.
     * Articles are user-managed content and should not have default system entries.
     */
    public function up()
    {
        DB::table('articles')->delete();
    }

    public function down()
    {
        // Nothing to restore — sample data should not exist
    }
}
