"""Reproduce with Blender 5.1.1, headless Cycles CPU, -noaudio. No GPU/display/audio.
The telekinesis source is immutable except camera and common camera-facing XP bindings.
Vein cells are exactly the eight extracted source coordinates, never relocated/scaled.
"""
import bpy, json, math, time, hashlib, random, os, sys
from pathlib import Path
from collections import deque
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
ROOT=Path(__file__).resolve().parent
TELE={
    '01-left-sweep':{'azimuth_delta_degrees':-4,'elevation_delta_degrees':0,'distance_multiplier':1},
    '02-right-sweep':{'azimuth_delta_degrees':10,'elevation_delta_degrees':0,'distance_multiplier':1},
    '03-higher':{'azimuth_delta_degrees':0,'elevation_delta_degrees':8,'distance_multiplier':1},
    '04-wider':{'azimuth_delta_degrees':0,'elevation_delta_degrees':0,'distance_multiplier':1.10},
}
VEIN={'01-three-cells-burst':3,'02-front-layer-cascade':4}

def plain(v):
    if isinstance(v,(str,int,float,bool)) or v is None:return v
    if isinstance(v,bpy.types.ID):return {'ID':v.name,'type':v.bl_rna.identifier}
    if hasattr(v,'to_dict'):return {k:plain(x) for k,x in v.to_dict().items()}
    try:return [plain(x) for x in v]
    except TypeError:return str(v)

def rna(v,skip=()):
    out={}
    for p in v.bl_rna.properties:
        k=p.identifier
        if k in ('rna_type','session_uid') or k in skip or p.type=='COLLECTION':continue
        try:
            x=getattr(v,k)
            if p.type=='POINTER' and not isinstance(x,bpy.types.ID):continue
            out[k]=plain(x)
        except (AttributeError,TypeError):pass
    return out

def node_tree(tree):
    if not tree:return None
    return {'nodes':[{**rna(n,('location','dimensions','select')),'inputs':{i.identifier:plain(i.default_value) for i in n.inputs if hasattr(i,'default_value')},'color_ramp':{'elements':[(e.position,list(e.color)) for e in n.color_ramp.elements]} if hasattr(n,'color_ramp') else None} for n in tree.nodes],'links':sorted((l.from_node.name,l.from_socket.identifier,l.to_node.name,l.to_socket.identifier) for l in tree.links)}

def payload(include_bindings=True):
    s=bpy.context.scene;objects=[]
    for o in sorted(bpy.data.objects,key=lambda x:x.name):
        if o.type=='CAMERA':continue
        rec={'name':o.name,'type':o.type,'parent':o.parent.name if o.parent else None,'location':list(o.location),'rotation_mode':o.rotation_mode,'rotation_euler':list(o.rotation_euler),'rotation_quaternion':list(o.rotation_quaternion),'scale':list(o.scale),'matrix_parent_inverse':[list(r) for r in o.matrix_parent_inverse],'properties':plain(dict(o.items())),'visibility':{k:getattr(o,k) for k in ('hide_render','visible_camera','visible_shadow','visible_diffuse','visible_glossy','visible_transmission','visible_volume_scatter')}}
        rec['constraints']=[rna(c) for c in o.constraints if include_bindings or c.name!='XP camera-facing invariant binding']
        rec['modifiers']=[rna(m) for m in o.modifiers]
        if o.type=='MESH':
            m=o.data;rec['mesh']={'name':m.name,'vertices':[list(v.co) for v in m.vertices],'polygons':[list(p.vertices) for p in m.polygons],'material_indices':[p.material_index for p in m.polygons],'materials':[x.name for x in m.materials],'uvs':{u.name:[list(x.uv) for x in u.data] for u in m.uv_layers}}
        elif o.data:rec['data']=rna(o.data)
        objects.append(rec)
    mats=[{'name':m.name,'rna':rna(m,('preview',)),'nodes':node_tree(m.node_tree)} for m in sorted(bpy.data.materials,key=lambda x:x.name)]
    imgs=[{'name':i.name,'size':list(i.size),'source':i.source,'packed_sha256':hashlib.sha256(i.packed_file.data).hexdigest() if i.packed_file else None,'colorspace':i.colorspace_settings.name} for i in sorted(bpy.data.images,key=lambda x:x.name) if i.source=='FILE']
    return {'objects':objects,'materials':mats,'images':imgs,'world':{'rna':rna(s.world),'nodes':node_tree(s.world.node_tree)},'render':rna(s.render,('filepath',)),'cycles':rna(s.cycles),'view':rna(s.view_settings),'frame':s.frame_current}

def encoded(data):return json.dumps(data,sort_keys=True,separators=(',',':')).encode()
def sha(data):return hashlib.sha256(data).hexdigest()
def write_json(path,data):path.write_text(json.dumps(data,indent=2))

def camera_record():
    c=bpy.context.scene.camera
    return {'location_blender_xyz':list(c.location),'rotation_euler_radians_xyz':list(c.rotation_euler),'rotation_euler_degrees_xyz':[math.degrees(x) for x in c.rotation_euler],'matrix_world':[list(r) for r in c.matrix_world],'projection':c.data.type,'lens_mm':c.data.lens,'sensor_width_mm':c.data.sensor_width,'sensor_height_mm':c.data.sensor_height,'sensor_fit':c.data.sensor_fit,'shift_x':c.data.shift_x,'shift_y':c.data.shift_y,'clip_start':c.data.clip_start,'clip_end':c.data.clip_end,'horizontal_fov_degrees':math.degrees(c.data.angle_x)}

def bind_xp():
    for o in bpy.data.objects:
        if o.name.startswith('Vanilla XP camera billboard'):
            c=o.constraints.new('COPY_ROTATION');c.name='XP camera-facing invariant binding';c.target=bpy.context.scene.camera;c.target_space='WORLD';c.owner_space='WORLD';c.mix_mode='REPLACE'

def camera_variant(label):
    c=bpy.context.scene.camera;target=Vector((-.7,0,1));v=c.location-target;spec=TELE[label]
    distance=v.length*spec['distance_multiplier'];az=math.atan2(v.y,v.x)+math.radians(spec['azimuth_delta_degrees']);el=math.asin(v.z/v.length)+math.radians(spec['elevation_delta_degrees'])
    c.location=target+Vector((distance*math.cos(el)*math.cos(az),distance*math.cos(el)*math.sin(az),distance*math.sin(el)))
    c.rotation_euler=(target-c.location).to_track_quat('-Z','Y').to_euler();bpy.context.view_layer.update()
    return {**spec,'look_at_blender_xyz':list(target),**camera_record()}

def bbox(objects):
    s=bpy.context.scene;points=[world_to_camera_view(s,s.camera,o.matrix_world@v.co) for o in objects if o.type=='MESH' for v in o.data.vertices]
    return [min(p.x for p in points),min(p.y for p in points),max(p.x for p in points),max(p.y for p in points)]

def ray_visible(point,ignore=()):
    s=bpy.context.scene;origin=s.camera.location;direction=point-origin;distance=direction.length;direction.normalize()
    for o in bpy.data.objects:
        if o.type!='MESH' or o.hide_render or o.name in ignore or o.name.startswith(('Vanilla XP','ARROW','Cascade fragment','Cascade diamond')):continue
        inv=o.matrix_world.inverted();hit,loc,normal,index=o.ray_cast(inv@origin,(inv.to_3x3()@direction).normalized())
        if hit and (o.matrix_world@loc-origin).length<distance-.006:return False
    return True

def validate_tele():
    bpy.context.view_layer.update();s=bpy.context.scene;out={}
    for name in ('player','zombie','skeleton'):
        objects=[o for o in bpy.data.objects[name+' entity yaw'].children_recursive if o.type=='MESH'];box=bbox(objects)
        assert 0<box[0]<box[2]<1 and 0<box[1]<box[3]<1,(name,box)
        out[name+'_bbox']=box
    out['XP']=[]
    for o in bpy.data.objects:
        if o.name.startswith('Vanilla XP camera billboard'):
            dot=(o.matrix_world.to_3x3()@Vector((0,0,1))).normalized().dot((s.camera.matrix_world.to_3x3()@Vector((0,0,1))).normalized())
            assert dot>.999999
            assert len(o.data.vertices)==4 and len(o.data.polygons)==1 and all(v.co.z==0 for v in o.data.vertices)
            visible=ray_visible(o.matrix_world@Vector((0,.075,0)))
            assert visible,('Hidden XP',o.name)
            out['XP'].append({'name':o.name,'normal_camera_dot':dot,'visible':visible,'bbox':bbox([o])})
    for o in bpy.data.objects:
        if o.name.startswith('ARROW head'):assert len(o.data.vertices)==3
    out['fancy_items']=[{'name':o.name,'visible_shadow':o.visible_shadow,'polygons':len(o.data.polygons)} for o in bpy.data.objects if o.name.startswith('Fancy generated')]
    out['Steve_yaw_degrees']=math.degrees(bpy.data.objects['player entity yaw'].rotation_euler.z)
    return out

FACES=[((0,-1,0),(0,1,5,4)),((1,0,0),(1,2,6,5)),((0,1,0),(2,3,7,6)),((-1,0,0),(3,0,4,7)),((0,0,1),(4,5,6,7)),((0,0,-1),(0,3,2,1))]

def cube_mesh(name,lo,material,face_filter=None,expand=0):
    x,y,z=[v-expand for v in lo];X,Y,Z=[v+1+expand for v in lo]
    vertices=[(x,y,z),(X,y,z),(X,Y,z),(x,Y,z),(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]
    faces=[f for normal,f in FACES if face_filter is None or normal in face_filter]
    m=bpy.data.meshes.new(name);m.from_pydata(vertices,[],faces);m.update();o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o);m.materials.append(material)
    uv=m.uv_layers.new(name='UVMap')
    for poly in m.polygons:
        for i,co in zip(poly.loop_indices,[(0,0),(1,0),(1,1),(0,1)]):uv.data[i].uv=co
    return o

def crack_material(stage):
    name=f'Vein cascade Minecraft destroy stage {stage}';m=bpy.data.materials.new(name);m.use_nodes=True
    n=m.node_tree.nodes;l=m.node_tree.links;n.clear();out=n.new('ShaderNodeOutputMaterial');shader=n.new('ShaderNodeBsdfPrincipled');shader.inputs['Roughness'].default_value=.85;shader.inputs['Specular IOR Level'].default_value=.05
    ore=n.new('ShaderNodeTexImage');ore.image=bpy.data.images.load(str(ROOT/'assets/deepslate_diamond_ore.png'),check_existing=True);ore.interpolation='Closest'
    crack=n.new('ShaderNodeTexImage');crack.image=bpy.data.images.load(str(ROOT/'assets'/f'destroy_stage_{stage}.png'),check_existing=True);crack.interpolation='Closest'
    mul=n.new('ShaderNodeMixRGB');mul.blend_type='MULTIPLY';mul.inputs[0].default_value=1;l.new(ore.outputs['Color'],mul.inputs[1]);l.new(crack.outputs['Color'],mul.inputs[2]);l.new(mul.outputs[0],shader.inputs['Base Color']);l.new(shader.outputs[0],out.inputs[0])
    return m

def vein_variant(label):
    data=json.loads((ROOT/'vein-miner/source-extraction.json').read_text());cells=[tuple(p) for p in data['vein']['coordinates_mc_xyz']];anchor=data['anchor_mc_xyz'];start=(-13,-25,167)
    source={tuple(c[:3]):data['palette'][c[3]] for c in data['cells']}
    assert all(source[p]['Name']=='minecraft:deepslate_diamond_ore' for p in cells)
    distance={start:0};q=deque([start])
    while q:
        p=q.popleft()
        for n in cells:
            if n not in distance and sum(abs(a-b) for a,b in zip(p,n))==1:distance[n]=distance[p]+1;q.append(n)
    assert len(distance)==8
    order=[(-13,-25,167),(-13,-24,167),(-13,-24,166),(-13,-25,166)];broken=set(order[:VEIN[label]])
    for o in list(bpy.data.objects):
        coord=tuple(o.get('minecraft_coordinate',[]))
        if (o.name.startswith('World ') and coord in cells) or o.name.startswith(('Breaking','Correct target mining fragment','Vanilla terrain mining fragment')):bpy.data.objects.remove(o,do_unlink=True)
    def convert(p):return Vector((p[0]-anchor[0],p[2]-anchor[2],p[1]-anchor[1]))
    records=[];oremat=bpy.data.materials['minecraft:block/deepslate_diamond_ore'];rng=random.Random(2026091406+len(broken))
    for p in sorted(cells,key=lambda p:(distance[p],p)):
        stage=max(0,9-distance[p]);lo=convert(p);rec={'minecraft_xyz':list(p),'source_block':source[p]['Name'],'source_blender_bounds':[list(lo),list(lo+Vector((1,1,1)))],'BFS_distance_from_mined_cell':distance[p],'break_stage':9 if p in broken else stage,'state':'already_broken_with_drops' if p in broken else 'actively_cracking','source_cell_removed':p in broken,'particle_objects':[],'drop_objects':[]}
        if p not in broken:
            o=cube_mesh('Vein cell '+str(p)+' active stage '+str(stage),lo,crack_material(stage));o['minecraft_coordinate']=p;o['blockstate']=json.dumps(source[p]);o['break_stage']=stage;o['vein_source']=True;rec['mesh_object']=o.name
        # Every exact vein cell produces real ore-textured 3D mining chips, routed out
        # its west face through the opened front layer, with source tags retained.
        front=Vector((min(lo.x,2)-.06,lo.y+.5,lo.z+.5))
        count=16 if p in broken else 10
        for j in range(count):
            loc=front+Vector((rng.uniform(-.72,-.04),rng.uniform(-.44,.44),rng.uniform(-.40,.46)))
            size=rng.uniform(.027,.072);bpy.ops.mesh.primitive_cube_add(size=size,location=loc);o=bpy.context.object;o.name='Cascade fragment '+str(p)+' '+str(j);o.data.materials.append(oremat);o.rotation_euler=[rng.random()*3 for _ in range(3)];o['source_vein_cell']=p
            u=rng.randrange(13)/16;v=rng.randrange(13)/16
            for loop in o.data.uv_layers.active.data:loop.uv=(u+loop.uv.x/4,v+loop.uv.y/4)
            rec['particle_objects'].append(o.name)
        if p in broken:
            # Fancy 3D ore-block item drops: valid silk-touch yield, six real texture faces.
            for j in range(2):
                o=cube_mesh('Cascade diamond ore block drop '+str(p)+' '+str(j),Vector((-.5,-.5,-.5)),oremat);o.scale=(.19,.19,.19);o.location=front+Vector((-.30-.38*j,-.28+.53*j,.10-.23*j));o.rotation_euler=(.3+j*.4,.2,.45+j*.5);o['source_vein_cell']=p;o['item']='minecraft:deepslate_diamond_ore';o['yield_condition']='Silk Touch';rec['drop_objects'].append(o.name)
        records.append(rec)
    # Restore only faces newly exposed by removal of genuine ore cells. Neighbour
    # cells remain at original coordinates; no cutaway, translated vein, or new blocks.
    exposed=[]
    for p in broken:
        for normal,_ in FACES:
            mc_delta=(normal[0],normal[2],normal[1]);n=tuple(p[i]+mc_delta[i] for i in range(3))
            if n in cells or source.get(n,{}).get('Name','minecraft:air')=='minecraft:air':continue
            state=source[n];mat=bpy.data.materials.get(state['Name'].replace(':',':block/'))
            assert mat is not None
            face=tuple(-x for x in normal);o=cube_mesh('Newly exposed source face '+str(n)+' toward '+str(p),convert(n),mat,[face]);o['minecraft_coordinate']=n;o['blockstate']=json.dumps(state);exposed.append({'cell':n,'toward_broken':p,'object':o.name})
    bpy.context.view_layer.update()
    for rec in records:
        if rec['state']=='actively_cracking':
            o=bpy.data.objects[rec['mesh_object']];lo=Vector(rec['source_blender_bounds'][0]);visible=[]
            for normal,_ in FACES:
                point=lo+Vector((.5,.5,.5))+Vector(normal)*.503
                if Vector(normal).dot(bpy.context.scene.camera.location-point)>0 and ray_visible(point,[o.name]):visible.append(normal)
            rec['camera_visible_face_normals']=visible;rec['projected_bbox']=bbox([o])
        else:rec['projected_drop_bboxes']=[bbox([bpy.data.objects[n]]) for n in rec['drop_objects']]
    return {'source_extraction_sha256':sha((ROOT/'vein-miner/source-extraction.json').read_bytes()),'vein_size':8,'six_connected':True,'mined_source_cell':start,'broken_cell_count':len(broken),'unmoved_source_geometry':True,'all_eight_cells_in_progress_or_broken':all(r['particle_objects'] and (r['state']=='actively_cracking' or r['drop_objects']) for r in records),'cells':records,'newly_exposed_source_faces':exposed,'yield_note':'3D ore-block drops imply Silk Touch; texture and item are real Minecraft assets. This is a staged illustrative mining cascade, not a game screenshot.','camera':camera_record()}

def main(project,label):
    out=ROOT/project;bpy.ops.wm.open_mainfile(filepath=str(out/'approved-source.blend'));s=bpy.context.scene
    # Identical original 64-sample scene settings and seed. Fixed 2 CPU threads.
    assert s.render.engine=='CYCLES' and s.cycles.samples==64
    s.cycles.device='CPU';s.render.threads_mode='FIXED';s.render.threads=2
    assert s.render.resolution_x==1024 and s.render.resolution_y==1024
    details={'project':project,'label':label,'source_blend_sha256':sha((out/'approved-source.blend').read_bytes())}
    if project=='telekinesis':
        before=encoded(payload(False));bind_xp();assert encoded(payload(False))==before
        invariant=encoded(payload());details['camera']=camera_variant(label);assert encoded(payload())==invariant
        details['geometry']=validate_tele();details['source_non_camera_payload_sha256']=sha(before);details['variant_non_camera_payload_sha256']=sha(invariant);details['only_common_source_addition']='Identical COPY_ROTATION constraints on the three XP billboards; raw source transforms, meshes, UVs, materials, lights, world and rendering values unchanged.'
        (out/(label+'-non-camera.json')).write_bytes(invariant)
    else:
        details['vein']=vein_variant(label);assert details['vein']['all_eight_cells_in_progress_or_broken']
        write_json(out/(label+'-vein-cells.json'),details['vein'])
    bpy.ops.file.pack_all();assert all(i.packed_file for i in bpy.data.images if i.source=='FILE')
    details['packed_textures']=[{'name':i.name,'sha256':sha(i.packed_file.data)} for i in bpy.data.images if i.source=='FILE']
    s.render.filepath=str(out/(label+'.png'));bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(out/(label+'.blend')))
    details['reproduction']={'wrapper':label+'.py','implementation':'../render_variants.py','implementation_sha256':sha(Path(__file__).read_bytes()),'command':f'nix shell nixpkgs#blender --command blender --background -noaudio --python {label}.py','script_policy':'Saved local-only script; no portfolio commit or tracked-file edits.'}
    if '--build-only' in sys.argv:write_json(out/(label+'-build.json'),details);return
    started=time.perf_counter();bpy.ops.render.render(write_still=True);elapsed=time.perf_counter()-started
    image=bpy.data.images.load(s.render.filepath,check_existing=False);assert tuple(image.size)==(1024,1024)
    pixels=list(image.pixels)[::164];mean=sum(pixels)/len(pixels);variance=sum((v-mean)**2 for v in pixels)/len(pixels);assert variance>.001
    details['renderer']={'Blender':bpy.app.version_string,'engine':'Cycles','device':'CPU','samples':s.cycles.samples,'threads':s.render.threads,'duration_seconds':elapsed,'resolution':[1024,1024],'noaudio':True,'display_server':False,'GPU_allocated':False,'cgroup':Path('/proc/self/cgroup').read_text().strip(),'resource_policy':'Watchdog shared Blender cgroup: total cap 3 cores, CPUWeight 20.'}
    details['PNG']={'sha256':sha(Path(s.render.filepath).read_bytes()),'dimensions':list(image.size),'sample_variance':variance}
    write_json(out/(label+'-metadata.json'),details);print('VERIFIED_RENDER',project,label,elapsed,flush=True)
