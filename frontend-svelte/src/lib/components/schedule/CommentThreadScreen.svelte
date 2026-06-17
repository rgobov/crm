<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { user } from '$lib/stores/auth.js';

    export let appointmentId;

    let comments = [];
    let newMessage = '';
    let isLoading = true;
    let isSending = false;
    let scrollContainer;

    onMount(async () => {
        await loadComments();
    });

    async function loadComments() {
        isLoading = true;
        try {
            comments = await adminService.getCommentsForAppointment(appointmentId);
            setTimeout(scrollToBottom, 50);
        } catch (e) {
            console.error('Failed to load comments');
        } finally {
            isLoading = false;
        }
    }

    async function sendComment() {
        if (!newMessage.trim() || isSending) return;
        isSending = true;
        try {
            const saved = await adminService.addCommentToAppointment(appointmentId, newMessage.trim());
            comments = [...comments, saved];
            newMessage = '';
            setTimeout(scrollToBottom, 50);
        } catch (e) {
            alert('Ошибка при отправке');
        } finally {
            isSending = false;
        }
    }

    function scrollToBottom() {
        if (scrollContainer) scrollContainer.scrollTop = scrollContainer.scrollHeight;
    }

    function formatTime(isoStr) {
        return new Date(isoStr).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    }
</script>

<div class="chat-container">
    <div class="messages-area" bind:this={scrollContainer}>
        {#if isLoading}
            <div class="center"><span class="spinner"></span></div>
        {:else if comments.length === 0}
            <div class="empty">Комментариев пока нет. Начните обсуждение!</div>
        {:else}
            {#each comments as msg}
                {@const isMe = msg.authorId === $user?.staffId}
                <div class="bubble-wrapper" class:is-me={isMe}>
                    <div class="bubble">
                        {#if !isMe}
                            <span class="author">{msg.authorName}</span>
                        {/if}
                        <p>{msg.text}</p>
                        <span class="time">{formatTime(msg.createdAt)}</span>
                    </div>
                </div>
            {/each}
        {/if}
    </div>

    <div class="input-area">
        <textarea
            bind:value={newMessage}
            placeholder="Ваш комментарий..."
            on:keydown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), sendComment())}
        ></textarea>
        <button class="send-btn" on:click={sendComment} disabled={!newMessage.trim() || isSending}>
            {isSending ? '...' : '✈️'}
        </button>
    </div>
</div>

<style>
    .chat-container { display: flex; flex-direction: column; height: 100%; background: #f8fafc; }

    .messages-area { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 12px; }

    .bubble-wrapper { display: flex; width: 100%; }
    .bubble-wrapper.is-me { justify-content: flex-end; }

    .bubble {
        max-width: 80%; padding: 12px 16px; border-radius: 18px;
        background: white; box-shadow: 0 2px 5px rgba(0,0,0,0.05);
        position: relative;
    }
    .is-me .bubble { background: var(--primary-color); color: white; border-bottom-right-radius: 4px; }
    .bubble-wrapper:not(.is-me) .bubble { border-bottom-left-radius: 4px; }

    .author { display: block; font-size: 10px; font-weight: 800; color: var(--primary-color); margin-bottom: 4px; text-transform: uppercase; }
    p { margin: 0; font-size: 14px; line-height: 1.4; }
    .time { display: block; font-size: 9px; text-align: right; margin-top: 4px; opacity: 0.6; }

    .input-area {
        padding: 16px; background: white; border-top: 1px solid #f1f5f9;
        display: flex; gap: 12px; align-items: flex-end;
    }
    textarea {
        flex: 1; padding: 12px; border: 1.5px solid #f1f5f9; border-radius: 16px;
        font-size: 14px; outline: none; background: #f8fafc; resize: none; max-height: 100px;
    }
    .send-btn {
        width: 44px; height: 44px; background: var(--primary-gradient);
        color: white; border: none; border-radius: 14px; cursor: pointer;
        font-size: 18px; display: flex; align-items: center; justify-content: center;
    }
    .send-btn:disabled { opacity: 0.5; }

    .empty { text-align: center; color: #94a3b8; padding: 40px; font-size: 13px; }
    .center { display: flex; justify-content: center; padding: 20px; }
</style>
