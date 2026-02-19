<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateNotificationTables extends Migration
{
    public function up()
    {
        Schema::dropIfExists('user_notifications');
        Schema::dropIfExists('notifications');

        Schema::create('notifications', function (Blueprint $table) {
            $table->id();
            $table->string('title');
            $table->string('message');
            $table->string('type')->default('info');
            $table->string('action_url')->nullable();
            $table->foreignId('document_id')->nullable()->constrained()->onDelete('set null');
            $table->timestamps();
        });

        Schema::create('user_notifications', function (Blueprint $table) {
            $table->foreignId('user_id')->constrained()->onDelete('cascade');
            $table->foreignId('notification_id')->constrained()->onDelete('cascade');
            $table->boolean('is_read')->default(false);
            $table->timestamps();
            $table->primary(['user_id', 'notification_id']);
        });
    }

    public function down()
    {
        Schema::dropIfExists('user_notifications');
        Schema::dropIfExists('notifications');
    }
}
