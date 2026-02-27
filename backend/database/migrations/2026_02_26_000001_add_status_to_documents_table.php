<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddStatusToDocumentsTable extends Migration
{
    public function up()
    {
        Schema::table('documents', function (Blueprint $table) {
            // draft   = private, only visible to creator (not yet submitted)
            // pending = submitted and waiting for HR/Admin review
            // approved = visible to all users
            // rejected = only visible to the creator
            $table->enum('status', ['draft', 'pending', 'approved', 'rejected'])->default('pending')->after('is_active');
            $table->unsignedBigInteger('reviewed_by')->nullable()->after('status');
            $table->timestamp('reviewed_at')->nullable()->after('reviewed_by');
            $table->text('review_note')->nullable()->after('reviewed_at');
            $table->foreign('reviewed_by')->references('id')->on('users')->onDelete('set null');
        });
    }

    public function down()
    {
        Schema::table('documents', function (Blueprint $table) {
            $table->dropForeign(['reviewed_by']);
            $table->dropColumn(['status', 'reviewed_by', 'reviewed_at', 'review_note']);
        });
    }
}
