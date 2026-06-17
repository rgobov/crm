<script>
    import ResourceEditScreen from '$lib/components/resources/ResourceEditScreen.svelte';
    import { page } from '$app/stores';

    const id = $page.params.id;
</script>

<ResourceEditScreen resourceId={id === 'new' ? null : id} />
