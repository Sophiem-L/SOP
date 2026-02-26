<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class ReviewDocuments extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('documents', function (Blueprint $table) {
        $table->tinyInteger('status')->default(0)->after('is_active'); // 0:private, 1:public, 2:Approved, 3:Rejected
        $table->foreignId('reviewed_by')->nullable()->after('status')->constrained('users')->onDelete('set null');
        $table->timestamp('reviewed_at')->nullable()->after('reviewed_by');
         });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('documents', function (Blueprint $table) {
            //
        });
    }
}
