#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec2 tc;
in vec4 shadow_coord;
in vec3 vertEyeSpacePos;

out vec4 fragColor;

struct PositionalLight
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};

struct Material
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;

layout (binding=0) uniform samplerCube tex_map; //Texture
layout (binding=1) uniform sampler2D samp;      //Not used
layout (binding=9) uniform sampler2DShadow shadowTex; //Shadow texture

//This is only used on the mountain
void main(void){
	// normalize the light, normal, and view vectors:
    vec3 L = normalize(varyingLightDir);
    vec3 N = normalize(varyingNormal);
    vec3 V = normalize(-varyingVertPos);

    //FOG Stuff
    vec4 fog = vec4(0.7, 0.8, 0.9, 1.0);	//Color
    float fogStart = 0.2;
    float fogEnd = 900;
    float dist = length(vertEyeSpacePos.xyz);
    float fogFactor = clamp(((fogEnd-dist)/(fogEnd - fogStart)), 0.0, 1.0);

    //Reflection vector computed in similar way to what was done for lighting
    vec3 r = -reflect(normalize(-varyingVertPos), normalize(varyingNormal));

    float notInShadow = textureProj(shadowTex, shadow_coord);

    // get the angle between the light and surface normal:
    float cosTheta = dot(L,N);

    // halfway vector varyingHalfVector was computed in the vertex shader,
    // and interpolated prior to reaching the fragment shader.
    // It is copied into variable H here for convenience later.
    vec3 H = normalize(varyingHalfVector);

    // get angle between the normal and the halfway vector
    float cosPhi = dot(H,N);

    // compute ADS contributions (per pixel):
    vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
    vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
    vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess*3.0);

    //vec4 varColor = globalAmbient * material.ambient + light.ambient * material.ambient;
    vec4 texel = texture(tex_map, r);


    //fragColor = texel * 0.5 +  vec4((ambient + diffuse + specular), 1.0) * 0.5;
    fragColor = vec4((ambient), 1.0) * 0.6 + texel * 0.4;

    if(notInShadow == 1.0) {
        //TEST: Changed from varColor to fragColor
        fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
        + light.specular * material.specular
        * pow(max(dot(H,N),0.0),material.shininess*3.0);

        //BOTT'M GEAR TESTING M8
        //testing if the shadows are working - outputs white to the pixels in light
        //fragColor = vec4(1.0, 1.0, 1.0, 1.0);
    }

    fragColor = mix(fog, fragColor, fogFactor);
    //Output color retrieved from texture (now cube map) with
    //  lookup texture coordinate now being reflection vector
    //fragColor = varColor * 0.5 + texture(tex_map, r) * 0.5;        //Commented this out for the time being
}